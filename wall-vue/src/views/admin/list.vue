<template>

    <a-space direction="vertical" style="width: 100%">
        <a-form :model="searchForm" ref="searchForm" name="searchForm" layout="inline" style="float: left">
            <a-form-item name="title" label="标题" style="margin-bottom: 10px;" :style="this.width">
                <a-input v-model:value="searchForm.title" placeholder="标题"></a-input>
            </a-form-item>

            <a-form-item name="title" label="标签" style="margin-bottom: 10px" :style="this.width">
                <a-select placeholder="标签" allowClear="true" v-model:value="searchForm.tagId" style="width: 200px"  :style="this.width">
                    <template  v-for="(item, i) in tagList">
                        <a-select-option :value="item.tagId">{{item.tagName}}</a-select-option>
                    </template>
                </a-select>
            </a-form-item>

            <a-space style="margin-bottom: 10px">
                <a-button type="primary" @click="searchTag(this.pagination.current,this.pagination.pageSize)"><search-outlined /> 查询</a-button>
                <a-button type="primary" @click="this.$router.push('/admin/upload')"><tags-outlined /> 添加</a-button>
                <a-button type="primary" danger @click="deletes"><delete-outlined /> 批量删除</a-button>
            </a-space>
        </a-form>


        <a-table :row-selection="{onChange: (selectedRowKeys, selectedRows, event) => {this.recordRowKey(selectedRowKeys,selectedRows,event)},}"
                 :rowKey="record=>record.resourceId"
                 :dataSource="dataSource"
                 :columns="columns"
                 :pagination="pagination"
                 :loading="false"
                 :locale="{emptyText:'暂无数据，赶快添加吧！'}"
                 :scroll="{ x: 850 }"
        >

            <template #bodyCell="{ text, record, index, column }">
                <template v-if="column.key === 'number'">
                    {{index+1}}
                </template>

                <template v-if="column.key === 'coverPath'">
                    <a-image style="border-radius: 5px" :previewMask="true" :width="60" :src="'/api/static/'+text" alt="图片不存在"/>
                </template>

                <template v-if="column.key === 'resourceType'">
                    <a-tag color="success">{{text}}</a-tag>
                </template>

                <template v-if="column.key === 'coverType'">
                    <div v-if="text == '1'">
                        自动识别
                    </div>
                    <div v-else-if="text == '2'">
                        上传封面
                    </div>
                </template>

                <template v-if="column.key === 'visibleFlag'">
                    <a-switch :checked="text" @change="updateVisible(record)" />
                </template>

                <!--<template v-if="column.key === 'operation'">
                    <a-space>
                        <a href="javascript:" @click="editTag(record)">详情</a>
                    </a-space>
                </template>-->
            </template>
        </a-table>
    </a-space>
</template>
<script>
    import { ref, defineComponent } from 'vue';
    import {get,post, remove} from "@/utils/request";
    import {message, Modal} from "ant-design-vue";

    export default defineComponent({
        components: {},
        data(){
            return {
                searchForm: {
                    title: "",
                    tagId: ref()
                },
                detailShow:false,
                columns: [
                    {
                        title: '封面',
                        dataIndex: 'coverPath',
                        key: 'coverPath',
                        width:80
                    },
                    {
                        title: '标题',
                        dataIndex: 'title',
                        key: 'title',
                    },
                    {
                        title: '文件类型',
                        dataIndex: 'resourceType',
                        key: 'resourceType',
                        align:'center',
                        width:100
                    },
                    {
                        title: '封面类型',
                        dataIndex: 'coverType',
                        key: 'coverType',
                        width: 120,
                        align:'center'
                    },
                    {
                        title: '创建时间',
                        dataIndex: 'createTime',
                        key: 'createTime',
                        align:'center',
                        width:200
                    },
                    {
                        title: '可见性',
                        dataIndex: 'visibleFlag',
                        key: 'visibleFlag',
                        align:'center',
                        width: 120,
                    },
                    /*{
                        title: '操作',
                        dataIndex: 'operation',
                        key: 'operation',
                        width: 90,
                        align:'center',
                        fixed: 'right',
                    },*/

                ],
                dataSource: [],
                pagination:{
                    total: 0,
                    current: 1,
                    pageSize: 10    ,
                    onChange: (current, pageSize) => {
                        this.searchTag(current,pageSize);
                    }
                },

                checkedState:false,
                deleteResourceId:"",
                tagList:[]

            }
        },
        created() {
            get('/t-tag/query').then((res) => {
                this.tagList = res.data
            }).catch(() => {
            });
            this.searchTag()
        },
        methods:{
            checkedState(e){
                return true
            },
            updateVisible(e){
                console.log(e)
                e.visibleFlag = !e.visibleFlag

                post("/t-resource/update?resourceId="+e.resourceId+"&visibleFlag="+e.visibleFlag).then((res)=>{
                    console.log(res.data)
                },(err) => {

                })
            },
            searchTag(pageNo,pageSize){
                if(typeof pageNo == "undefined" || pageNo == "" ){
                    pageNo = this.pagination.current
                }

                if(typeof pageSize == "undefined" || pageSize == "" ){
                    pageSize = this.pagination.pageSize
                }
                this.NProgress.start()
                var tagId = "";
                if (typeof this.searchForm.tagId != "undefined"){
                    tagId = this.searchForm.tagId;
                }

                get("/t-resource/list?title="+this.searchForm.title+"&tagId="+tagId+"&pageNo="+pageNo+"&pageSize="+pageSize).then((res)=>{
                    this.pagination.total = res.data.total
                    this.pagination.current = pageNo;
                    this.dataSource = res.data.records
                    this.NProgress.done()
                })
            },
            recordRowKey(e){
                this.deleteResourceId = e;
            },
            deletes(){
                var that = this;
                if(this.deleteResourceId.length == 0){
                    message.warning('你还没选择要删除的图片或视频呢');
                    return false;
                }
                Modal.confirm({
                    title: '你确定要删除这些资源吗？相关本地文件也会被清除掉，不可恢复哦！',
                    okText:'嗯，确定',
                    cancelText:'我点错啦',
                    onOk() {
                        that.NProgress.start()
                        remove("/t-resource/delete?resourceIds="+that.deleteResourceId).then((res)=>{
                            that.searchTag();
                            that.deleteTagId=[]
                            message.success("资源和文件删除完成");
                            that.NProgress.done()
                        },(err) => {

                        })
                    },
                });
            }
        },
    });
</script>
<style >

</style>
