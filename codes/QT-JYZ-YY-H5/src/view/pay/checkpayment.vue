<template>
  <div class="pay mt55" style="height: 100%; width: 100%">
    <headerpu>
      <i
        class="el-icon-arrow-left backicon"
        @click.stop="goDetail"
        slot="back"
      ></i>
      <div class="grid-content bg-purple-dark" slot="title">支付订单</div>
    </headerpu>
    <div>
      <el-main>
        <p class="itemstyle1">
          总计: <span>￥{{ list.price_total }}</span>
        </p>
        <p class="price"><sub>￥</sub>{{ list.pay_price }}</p>
        <!-- <div class="flex itemstyle2">
          <span class="flex1">油卡 (余额：￥{{ balance }})</span>
          <span>-￥{{ balance_pay }}</span>
        </div> -->
        <div>
          <!-- <div class="flex flex-y-center itemstyle3">
            <div class="flex1 flex-y-center">
              <img src="../../assets/images/wechatpay.png" alt="" />
              <span>微信支付</span>
            </div>
            <div>
              <el-radio v-model="radio" label="1"> </el-radio>
            </div>
          </div> -->
          <div class="flex flex-y-center itemstyle3">
            <div class="flex1 flex-y-center">
              <img src="../../assets/images/alipay.png" alt="" />
              <span>支付宝支付</span>
            </div>
            <div>
              <el-radio v-model="radio" label="2"> </el-radio>
            </div>
          </div>
        </div>
      </el-main>
      <div class="btn-box">
        <button class="button-style" @click="pay">确认支付</button>
      </div>
    </div>
  </div>
</template>

<script>
import headerpu from "@/components/commont/header";
import { getororderpay, getcheckPay, getorderDetai } from "../../network/order";
export default {
  data() {
    return {
      radio: "2",
      id: 1,
      type: 0,
      list: {},
      balance_pay: "",
      balance: "",
      value: false,
      lastid: 0,
      timer: "",
    };
  },
  created() {
    this.id = this.$route.query.id;
    this.getorderDetais();
    this.checkpay();
    this.lastid = window.localStorage.getItem("lastid");
    this.balance = window.localStorage.getItem("balance");
    this.balance_pay = window.localStorage.getItem("balance_pay");
  },
  mounted() {},
  watch() {},
  methods: {
    goDetail() {
      clearInterval(this.timer);
      this.$router.push({
        path: "/ordersuccess",
        query: {
          id: this.id,
        },
      });
    },
    pay() {
      this.$message.error("支付功能暂未开放");
      return;
      getororderpay(this.id).then((res) => {
        if (res.status !== 200) {
          this.$message.error(res.data.msg);
        }
        if (res.data.status == 607) {
          this.$message.error(res.data.msg);
        } else if (res.data.status == 608) {
          this.$message.error(res.data.msg);
          clearInterval(this.timer);
          setTimeout(() => {
            this.$router.push({
              path: "/login",
            });
          }, 1200);
        } else {
          window.location.href =
            "alipays://platformapi/startapp?appId=20000067&url=" +
            res.data.data.payUrl +
            "";
        }
      });
    },
    checkpay() {
      this.timer = setInterval(() => {
        getcheckPay(this.id).then((res) => {
          if (res.status !== 200) {
            this.$message.error(res.data.msg);
          }
          if (res.data.status == 607) {
            this.$message.error(res.data.msg);
          } else if (res.data.status == 608) {
            this.$message.error(res.data.msg);
            clearInterval(this.timer);
            setTimeout(() => {
              this.$router.push({
                path: "/login",
              });
            }, 1200);
          } else {
            this.type = res.data.data.status;
            if (this.type == 2) {
              if (this.id == window.localStorage.getItem("lastid")) {
                clearInterval(this.timer);
                this.$router.push({
                  path: "/",
                });
              } else {
                window.localStorage.setItem("lastid", this.id);
                setTimeout(() => {
                  this.$router.push({
                    path: "/paysuccess",
                  });
                }, 500);
                clearInterval(this.timer);
              }
            }
          }
        });
      }, 2000);
    },

    getorderDetais() {
      getorderDetai(this.id).then((res) => {
        if (res.status !== 200) {
          this.$message.error(res.data.msg);
        }
        if (res.data.status == 607) {
          this.$message.error(res.data.msg);
        } else if (res.data.status == 608) {
          this.$message.error(res.data.msg);
          clearInterval(this.timer);
          setTimeout(() => {
            this.$router.push({
              path: "/login", //跳转的路径
            });
          }, 1200);
        } else {
          this.list = res.data.data;
        }
      });
    },
  },
  components: {
    headerpu: headerpu,
  },
};
</script>
<style scoped>
.mt1rem {
  margin-top: 1rem;
}
.itemstyle1 {
  text-align: center;
  color: #cccccc;
  margin-bottom: 0.5rem;
  font-size: 1rem;
}
.price {
  color: #000000;
  font-size: 3.5rem;
  text-align: center;
  font-weight: bold;
  display: flex;
  justify-content: center;
  align-items: baseline;
}
.price sub {
  font-size: 2rem;
}
.itemstyle2 {
  font-size: 1.4rem;
  margin: 3rem 0;
  color: #000;
}
.itemstyle3 {
  margin-bottom: 2rem;
  font-size: 1.4rem;
}
.itemstyle3 img {
  width: 3rem;
  height: 3rem;
  margin-right: 1.5rem;
}
.btn-box {
  margin: 0 20px;
}
.button-style {
  width: 90%;
  margin-bottom: 0;
  position: fixed;
  bottom: 3.5rem;
  font-size: 1.4rem;
  padding: 0.8rem;
}
>>> .el-radio__label {
  display: none;
}
>>> .el-radio__inner {
  width: 20px;
  height: 20px;
}
>>> .el-radio__input.is-checked .el-radio__inner {
  background: var(--active) !important;
  border-color: var(--active) !important;
}
>>> .el-radio__inner::after {
  width: 7px;
  height: 7px;
}
</style>
