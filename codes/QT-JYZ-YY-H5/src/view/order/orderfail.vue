<template>
  <div class="orderfail mt55">
    <headerpu>
      <i class="el-icon-arrow-left backicon" slot="back"></i>
      <div class="grid-content bg-purple-dark" slot="title">优惠券</div>
    </headerpu>
    <div class="content">
      <div class="order-success-top">
        <el-main><span v-if="flag !== false"> 退款中</span> <span v-else> 退款失败</span><i class="el-icon-check"></i></el-main>
      </div>
      <el-main>
        <el-row>
          <el-col :span="24"> <h4 class="order-title">订单详情</h4> </el-col>
        </el-row>
        <el-row>
          <el-col :span="24">
            <div class="border">
              <p class="flex-y-center">
                <span class="flex1">订单号:</span>
                <span>{{ list.number }}</span>
              </p>
              <p class="flex-y-center">
                <span class="flex1">下单时间:</span>
                <span>{{ list.date }}</span>
              </p>
              <p class="flex-y-center">
                <span class="flex1">加油站</span>
                <span>{{ list.jyz }}</span>
              </p>
              <p class="flex-y-center">
                <span class="flex1">油号</span>
                <span>{{ list.yh }}</span>
              </p>
              <p class="flex-y-center">
                <span class="flex1">油枪</span>
                <span>{{ list.yq }}</span>
              </p>
              <p class="flex-y-center">
                <span class="flex1">加油升数</span>
                <span>{{ list.oiling }} L</span>
              </p>
            </div>
            <div class="border">
              <p class="flex-y-center">
                <span class="flex1">原价</span>
                <span>￥{{ list.originalprice }}</span>
              </p>
              <p class="flex-y-center">
                <span class="flex1">优惠</span>
                <span class="redcolor">-￥{{ list.Discount }}</span>
              </p>
              <p class="flex-y-center" >
                <span class="flex1">优惠券</span>
                <span>-￥{{ list.coupon }}</span>
              </p>
            </div>
            <div class="border" >
              <p class="flex-y-center">
                <span class="flex1">支付金额</span>
                <span class="orderpay">￥{{ list.pay }}</span>
              </p>
            </div>
          </el-col>
        </el-row>
      </el-main>
    </div>
    <el-footer>
      <div v-if="flag !== false" class="flex-y-center">
        <el-button  class="button-style"
          >立即支付</el-button >
        <el-button class="button-styles" @click="dial(18859250954)" >联系门店</el-button>
      </div>
    </el-footer>
    <!-- 弹窗 -->
    <servicedialog :show.sync="dialogVisible">
        <div class="Message mb5remserve" slot="content">
          <i class="el-icon-warning meaasgeicon redcolor"></i>
         <span class="redcolor" style="font-weight: bold; font-size: 1.5rem"  >请联系门店管理员</span >
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button @click="dial(18859250954)" class="button-styles" >拨打电话</el-button>
        </span>
        </servicedialog>
  </div>
</template>
<script>
import servicedialog from "@/components/commont/dialog";
import headerpu from "@/components/commont/header";
export default {
  name: "orderfail",
  data() {
    return {
       dialogVisible: false,
      flag: true,
      form: {
        resource: "",
      },
      list: {
        number: "121212112487878",
        date: "2020.01.08.17:40",
        jyz: "中胜石油经十路站",
        yh: "92#",
        yq: "2",
        oiling: "14.99",
        originalprice: 100,
        Discount: 10,
        coupon: 20,
        pay: 90,
      },
    };
  },
  props: {},
  watch: {},
  methods: {
    onSubmit() {
    },
    score(){
      this.dialogVisible=true
    },
    dial(phoneNumber){
      this.dialogVisible=false
        window.location.href = "tel://" + phoneNumber;
    }
  },
  components: {
    headerpu: headerpu,
    servicedialog:servicedialog,
  },
};
</script>
<style scoped>
.header {
  background: var(--white);
}

.border {
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.12), 0 0 6px rgba(0, 0, 0, 0.04);
  padding: 1rem;
  border-radius: 1rem;
  margin-bottom: 2rem;
}
.el-radio {
     margin-bottom: 2rem;
}
.formtitle{
        margin-bottom: 2rem;
    font-size: 1.3rem;
}
.order-success-top {
  height: 9rem;
  background: var(--grey);
  color: #fff;
  font-size: 1.6rem;
  position: relative;
}
.order-success-top i {
     font-size: 3.5rem;
    font-weight: 900;
    background: #dddddd;
    border-radius: 50%;
    padding: 1rem;
    color: #bbbbbb;
    position: absolute;
    right: 0;
    bottom: 0.5rem;
}
.order-title {
  font-size: 1.6rem;
  margin-bottom: 1.5rem;
}
.border p {
  line-height: 3rem;
  font-size: 1.25rem;
}

.orderpay {
  color: var(--active);
  font-size: 1.7rem;
}
.el-footer {
  margin-bottom: 2rem;
}
.button-style,
.button-styles {
  margin-bottom: 0rem;
}
</style>
