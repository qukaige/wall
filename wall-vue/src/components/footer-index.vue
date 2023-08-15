<template>
    <a-layout-footer class="footer">
        <div style="min-height: 25px;overflow:hidden;line-height:25px;text-align:center;font-size:10px;">
            <a href="http://www.beian.gov.cn/portal/registerSystemInfo?recordcode=18042250" target="_blank" class="text-color">京ICP备18042250号</a>
        </div>
    </a-layout-footer>
</template>

<script>
    import {get} from "@/utils/request";

    export default {
        name: "footer",
        data(){
            return {
                beian:""
            }
        },
        created() {
            get("/t-option/target").then((res)=>{
                this.beian = res.data.beian;
            })
        }
    }
</script>

<style scoped>
    .footer{
        padding: 0px;
        background-color: #fff;
        margin-top: 50px;
        /* position: fixed; */
        bottom: 0px;
        width: 100%;
        z-index: 999;
    }
</style>
