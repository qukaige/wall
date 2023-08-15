package cn.ityao.wall.controller;


import cn.hutool.core.date.DateUtil;
import cn.ityao.wall.entity.TLog;
import cn.ityao.wall.entity.TOption;
import cn.ityao.wall.entity.TResource;
import cn.ityao.wall.entity.TTag;
import cn.ityao.wall.service.ITOptionService;
import cn.ityao.wall.service.ITResourceService;
import cn.ityao.wall.service.ITTagService;
import cn.ityao.wall.service.TLogService;
import cn.ityao.wall.util.DataResult;
import cn.ityao.wall.util.HttpClientUtil;
import cn.ityao.wall.util.IpUtils;
import cn.ityao.wall.util.StringUtils;
import com.alibaba.fastjson.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import java.util.*;

/**
 * <p>
 * 标签表 前端控制器
 * </p>
 *
 * @author tongyao
 * @since 2023-02-14
 */
@Slf4j
@RestController
@RequestMapping("/t-tag")
public class TTagController {


    @Autowired
    private ITTagService itTagService;

    @Autowired
    private ITResourceService itResourceService;

    @Autowired
    private ITOptionService itOptionService;

    @PostMapping("/saveOrUpdate")
    public DataResult saveOrUpdate(@Valid @RequestBody TTag tTag, HttpServletRequest request){
        String userName = (String) request.getAttribute("userName");
        if (tTag.getSort() == null){
            tTag.setSort(0);
        }
        if(StringUtils.isNotBlank(tTag.getTagId())){
            tTag.setModifyBy(userName);
            tTag.setModifyTime(new Date());
            itTagService.updateById(tTag);
        }else{
            tTag.setCreateBy(userName);
            tTag.setCreateTime(new Date());
            itTagService.save(tTag);
        }
        return DataResult.setSuccess(null);
    }

    @GetMapping("/list")
    public DataResult list(
            String tagName,
            @RequestParam(defaultValue = "1") int pageNo,
            @RequestParam(defaultValue = "10") int pageSize
    ){
        LambdaQueryWrapper<TTag> tTagLambdaQueryWrapper = new LambdaQueryWrapper<>();
        if(StringUtils.isNotBlank(tagName)){
            tTagLambdaQueryWrapper.like(TTag::getTagName,tagName);
        }
        tTagLambdaQueryWrapper.orderByAsc(TTag::getCreateBy,TTag::getSort);

        Page<TTag> page = new Page<>(pageNo,pageSize);
        IPage<TTag> iPage = itTagService.page(page,tTagLambdaQueryWrapper);
        return DataResult.setSuccess(iPage);
    }

    @DeleteMapping("/delete")
    public DataResult delete(String tagIds){
        List<String> tagId = Arrays.asList(tagIds.split(","));

        LambdaQueryWrapper<TResource> tResourceLambdaQueryWrapper = new LambdaQueryWrapper<>();
        for (String str : tagId){
            tResourceLambdaQueryWrapper.like(TResource::getTagId,str);
        }
        List<TResource> tResourceList = itResourceService.list(tResourceLambdaQueryWrapper);
        for (int i = 0; i < tResourceList.size(); i++) {
            for (int j = 0; j < tagId.size(); j++) {
                tResourceList.get(i).setTagId(tResourceList.get(i).getTagId().replaceAll(tagId.get(j)+",",""));
            }
        }
        if (tResourceList.size() != 0){
            itResourceService.updateBatchById(tResourceList);
        }

        Map map = new HashMap();
        map.put("option_key","initTagId");
        List<TOption> tOptionList = (List<TOption>) itOptionService.listByMap(map);
        TOption tOption = tOptionList.get(0);
        if (tagIds.indexOf(tOption.getOptionValue()) != -1){
            tOption = new TOption();
            tOption.setOptionKey("initTagId");
            tOption.setOptionValue("");
            itOptionService.updateById(tOption);
        }

        itTagService.removeByIds(tagId);
        return DataResult.setSuccess(null);
    }

    @Autowired
    private TLogService tLogService;

    @GetMapping("/query")
    public DataResult query(HttpServletRequest request){

        // 记录访问日志
        // 查询今天是否有记录

        String ipAddress = IpUtils.getIpAddress(request);
        LambdaQueryWrapper<TLog> tLogLambdaQueryWrapper = new LambdaQueryWrapper<>();
        tLogLambdaQueryWrapper.eq(TLog::getIp, ipAddress);
        tLogLambdaQueryWrapper.orderByDesc(TLog::getCreatetime);
        List<TLog> list = tLogService.list(tLogLambdaQueryWrapper);
        TLog tLog = new TLog();
        tLog.setIp(ipAddress);
        if (list != null && list.size() > 0) {
            TLog one = list.get(0);
            Date createtime = one.getCreatetime();
            String date1 = DateUtil.format(createtime, "yyyy-MM-dd");
            String date2 = DateUtil.format(new Date(), "yyyy-MM-dd");
            if (!date1.equals(date2)) {
                saveLog(tLog);
                log.info("访问记录 IP : " + ipAddress);
            } else {
                log.info("今日访问记录已记录, 不重复记录: " + ipAddress);
            }
        } else {
            saveLog(tLog);
            log.info("访问记录 IP : " + ipAddress);
        }

        LambdaQueryWrapper<TTag> tTagLambdaQueryWrapper = new LambdaQueryWrapper<>();
        tTagLambdaQueryWrapper.select(TTag::getTagId, TTag::getTagName);
        tTagLambdaQueryWrapper.orderByAsc(TTag::getCreateBy,TTag::getSort);
        return DataResult.setSuccess(itTagService.list(tTagLambdaQueryWrapper));
    }

    public static void main(String[] args) {
        String format = DateUtil.format(new Date(), "yyyy-MM-dd");
        System.out.println(format);

    }

    public void saveLog(TLog tLog) {
        String ip = tLog.getIp();
        JSONObject ipInfo = getIpInfo(ip);
        if (ipInfo != null) {
            String pro = ipInfo.getString("pro");
            String code = ipInfo.getString("cityCode");
            String city = ipInfo.getString("city");
            String address = ipInfo.getString("addr");
            tLog.setAddress(address);
            tLog.setCity(city);
            tLog.setCode(code);
            tLog.setProvince(pro);
        }
        if (tLog.getId() == null) {
            tLogService.save(tLog);
        } else {
            tLogService.updateById(tLog);
        }
    }

    public JSONObject getIpInfo(String ip) {
        try {
            String url = "http://whois.pconline.com.cn/ipJson.jsp?ip=" + ip + "&json=true";
            String result = HttpClientUtil.doGet(url, "GBK");
            return JSONObject.parseObject(result);
        } catch (Exception e) {
            log.error("解析IP失败:" + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }


    @GetMapping("/init")
    public void init() {
        List<TLog> list = tLogService.list();
        for (TLog tLog : list) {
            String province = tLog.getProvince();
            if (province == null) {
                saveLog(tLog);
            }
        }
    }
}
