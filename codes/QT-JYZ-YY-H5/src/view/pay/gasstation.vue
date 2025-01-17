<template>
  <div class="gasstation">
    <headerpu>
      <i class="el-icon-arrow-left backicon" slot="back"></i
    ></headerpu>
    <!-- 头部详细信息 -->
    <div class="top">
      <el-row>
        <el-col :span="24">
          <el-image class="top-bg" :src="logo"></el-image>
          <div class="wstyle topbg-style">
            <div class="top-content">
              <div class="flex">
                <el-image class="topimg1" :src="logo"></el-image>
                <div class="flex1">
                  <span class="title">{{ shop_name }}</span>
                  <div class="flex1 mt msg">
                    <div
                      :class="['mobile', contact_mobile ? '' : 'text-color']"
                    >
                      <!-- <i class="el-icon-phone-outline"></i> -->
                      <img src="../../assets/images/tele.png" alt="" />
                      <div>
                        <span>{{
                          contact_mobile ? contact_mobile : text
                        }}</span>
                      </div>
                    </div>
                    <div :class="['date', business_hours ? '' : 'text-color']">
                      <!-- <i class="el-icon-watch"></i> -->
                      <img src="../../assets/images/time.png" alt="" />

                      <div>
                        <span>{{
                          business_hours ? business_hours : text
                        }}</span>
                      </div>
                    </div>
                    <div
                      @click="space(lat01, lng01)"
                      :class="['address', address ? '' : 'text-color']"
                    >
                      <!-- <i class="el-icon-location-outline"></i> -->
                      <img src="../../assets/images/address.png" alt="" />

                      <div>
                        <span style="margin-right: 5px">{{ distance }}</span>
                        <span>{{ address ? address : text }}</span>
                      </div>
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </div>
        </el-col>
      </el-row>
    </div>
    <div>
      <div style="height: 1rem"></div>
      <!-- 油卡 -->
      <el-image
        :src="yk"
        @click="card"
        style="padding: 0 3.5%"
        v-if="ifRecharge == 1"
      >
        <div slot="error" class="image-slot">
          <img
            src="../../assets/images/5ca16463955d9.png"
            @click="card"
            alt=""
            style="width: 100%"
          />
        </div>
      </el-image>
      <div class="discount">
        <div class="dis-title">
          <div class="discount-box1">
            <span class="activity-number">{{ oil_number }}</span>
            <span class="price-reduction" v-if="showDiscount.havaDiscount"
              >￥{{ reduction }}</span
            >
          </div>
          <div class="discount-box2" v-if="showDiscount.havaDiscount">
            <span class="activity-desc">{{ showDiscount.desc }}</span>
            <span class="activity-icon">￥</span
            ><span class="activity-price">{{ showDiscount.price }}</span>
            <span class="activity-desc">起</span>
          </div>
          <div class="discount-box2" v-else>
            <span class="activity-icon">￥</span
            ><span class="activity-price">{{ reduction }}</span>
          </div>
        </div>

        <div v-if="yh.length > 0" class="activity-box">
          <div v-for="item in yh" :key="item.id" class="activity-item">
            <div class="activity-name">
              <span>{{ item.title }}</span>
            </div>
            <div class="activity-content">
              <span>{{ item.desc }}</span>
            </div>
          </div>
        </div>
        <div v-else>
          <!-- <p>暂无优惠!</p> -->
        </div>
      </div>
      <!-- 油号 -->
      <div class="oilnumber wstyle">
        <el-row>
          <el-col :span="24">
            <div class="flex-y-end number-header">
              <h4 class="flex1">油号</h4>
              <!-- <p>
                <span class="price-reduction">￥{{ reduction }}</span>
              </p> -->
            </div>
            <div class="number-content flex-wrap">
              <a
                href="javascript:;"
                v-for="(item, values) in oilnumber"
                :key="item.id"
                :data-value="values"
                :class="
                  activeClass == item.id
                    ? 'active number-content-item'
                    : 'number-content-item'
                "
                @click="getItme(item.id, item.type)"
              >
                {{ item.type }}
              </a>
            </div>
          </el-col>
        </el-row>
      </div>
      <!-- 油枪 -->
      <div class="oil-gun wstyle">
        <el-row>
          <el-col :span="24">
            <div class="flex-y-end gun-header">
              <h4 class="flex1">油枪</h4>
            </div>
            <div class="gun-content flex-wrap">
              <a
                href="javascript:;"
                v-for="(item, id) in oilgun"
                :key="item.id"
                :class="
                  activestyle == id
                    ? 'active number-content-item'
                    : 'number-content-item'
                "
                @click="getItmes(id, item.name)"
              >
                {{ item.name }}
              </a>
            </div>
            <h4 class="help">您可在现场与油站工作人员确认后再完成支付</h4>
          </el-col>
        </el-row>
      </div>
      <!-- 加油按钮 -->
      <div class="submitbtn wstyle">
        <el-button @click="jiayou" class="button-style jiayou-button"
          >立即加油</el-button
        >
        <el-drawer
          class="drawer"
          title="加油金额"
          :visible.sync="drawer"
          direction="btt"
          @close="closedrawer"
          size="unset"
        >
          <el-main>
            <el-row>
              <el-col :span="24">
                <el-input
                  v-model="price"
                  placeholder="请输入金额"
                  class="marginstyle title"
                  @input="changeinput"
                  maxlength="7"
                  oninput="value = value.replace(/^\D/g,'').replace(/[^\d.]/g,'').replace(/^(\-)*(\d+)\.(\d\d).*$/,'$1$2.$3').replace('.','$#$').replace(/\./g,'').replace('$#$','.')"
                ></el-input>
                <ul class="flex-y-center">
                  <li
                    class="lubricate-item"
                    v-for="item in movies"
                    :key="item.id"
                    @click="changeyh(item.price)"
                    :class="priceitem == item.price ? 'active ' : ''"
                  >
                    <a href="javascript:;"
                      ><p class="flex-y-base flex-x-center">
                        <sub class="sub">￥</sub><span>{{ item.price }}</span>
                      </p>
                      <p>
                        优惠<span>{{ item.discount_price }}</span
                        >元
                      </p></a
                    >
                  </li>
                </ul>

                <div v-if="have_discount.length != 0 && price != ''">
                  <div class="total-discount">
                    <span class="discount-title">优惠</span>
                    <p class="activecolor">
                      -￥<span>{{ Discount.give }}</span>
                    </p>
                  </div>

                  <div
                    class="lubricate-item1 flex-y-center"
                    v-for="item in Discount.have_discount"
                    :key="item.id"
                  >
                    <span class="flex1">{{
                      item.type === 3 ? "满减" : "升减"
                    }}</span>
                    <p class="activecolor">
                      {{ item.desc }} -￥<span>{{ item.give }}</span>
                    </p>
                    <!-- <router-link to="/discount">
                    <i class="el-icon-arrow-right"></i>
                  </router-link> -->
                  </div>
                </div>
                <div v-else>
                  <div class="total-discount no-discount">
                    <span class="activecolor">优惠</span>
                    <p class="activecolor">-￥<span>0</span></p>
                  </div>
                </div>
              </el-col>
            </el-row>
          </el-main>
          <el-footer>
            <el-row>
              <el-col :span="24">
                <div class="footer-button">
                  <div class="lubricate-item2 flex-y-center">
                    <div class="flex1 flex-y-base">
                      <span class="footer-icon">￥</span>
                      <span class="newprice">{{ Discount.pay_price }}</span>
                      <span
                        v-if="have_discount.length != 0 && price != ''"
                        class="oldprice"
                        >￥{{ price }}</span
                      >
                    </div>
                    <p>
                      约<span>{{ Discount.oil_l }}L</span>
                    </p>
                  </div>
                  <el-button @click="orderConfirm" class="footer-submit">提交订单</el-button>
                </div>
              </el-col>
            </el-row>
          </el-footer>
        </el-drawer>
      </div>
    </div>
    <!-- 弹窗 -->
    <div class="meaasge-dialog">
      <servicedialog :show.sync="dialogVisible">
        <div class="Message" slot="content">
          <i class="el-icon-warning meaasgeicon"></i>
          <p class="text-center" style="color: red; margin-bottom: 0.5rem">
            当前位置不在该加油站内
          </p>
          <p class="text-center" style="margin-bottom: 2rem">
            已选油站距离您过远，请确认是否支付到
          </p>
          <p style="display: flex; align-items: center; margin-bottom: 3rem">
            <img
              src="/static/jyimg.jpg"
              style="width: 7rem; height: 7rem; margin-right: 2rem"
              alt="图片"
            />
            <span style="font-weight: bold; font-size: 1.5rem"
              >中胜石油经十路站</span
            >
          </p>
        </div>
        <span slot="footer" class="dialog-footer">
          <el-button @click="pay" class="button-styles">继续支付</el-button>
          <el-button class="button-style" @click="dialogVisible = false"
            >重新选择油站</el-button
          >
        </span></servicedialog
      >
    </div>
  </div>
</template>

<script>
import headerpu from "@/components/commont/header";
import servicedialog from "@/components/commont/dialog";
import lubricate from "@/components/content/lubricate";
import { getOrderCreate, getOrderConfirm } from "../../network/order";
import { getgass, getsuggeprice, getDiscount } from "../../network/home";
export default {
  name: "gasstation",
  data() {
    return {
      id: 3,
      drawer: false,
      dialogVisible: false,
      distance: 4.3,
      activeClass: 1,
      activestyle: 0,
      address: "",
      contact_mobile: "",
      business_hours: "",
      shop_name: "",
      lat: "",
      lng: "",
      ifRecharge: 0,
      lat01: "",
      lng01: "",
      price: "",
      reduction: "",
      oilgunactive: "",
      oilgunId: 0,
      logo: "",
      dis_id_arr: "",
      priceitem: "",
      rzid: "",
      mch_id: "",
      oilnumber: [],
      oilgun: [],
      yh: [],
      movies: [],
      Discount: [],
      text: "商家暂未提供",
      // addloading: true,
      yk: "",
      showList: false,
      balance: "",
      oil_number: "",
      showDiscount: {},
      have_discount: [],
    };
  },
  created() {
    this.id = this.$route.query.id;
    this.lat = window.localStorage.getItem("lat");
    this.lng = window.localStorage.getItem("lng");
    this.getgas();
  },
  mounted() {
    // setTimeout(() => {
    //   this.addloading = false;
    // }, 1200);
  },

  methods: {
    // 优惠展开收缩
    showLists: function () {
      if (this.yh.length >= 3) {
        this.showList = !this.showList;
      } else {
        this.showList = false;
      }
    },
    // 油号切换
    getItme(id, values) {
      this.oilgunactive = values;
      this.oilgunId = id;
      for (var i = 0; i < this.oilnumber.length; i++) {
        if (this.oilnumber[i].type == this.oilgunactive) {
          this.oilgun = this.oilnumber[i].oil_gun;
          this.reduction = this.oilnumber[i].price;
          this.showDiscount = this.oilnumber[i].showDiscount;
          this.oil_number = this.oilnumber[i].type;
          this.yh = this.oilnumber[i].discountList;
          this.activestyle = 0;
          this.yqvalue = this.oilgun[0].name;
        }
      }
      this.activeClass = id;
      this.yhvalue = values;
    },
    // 油抢切换
    getItmes(id, valueitem) {
      this.activestyle = id;
      this.yqvalue = valueitem;
    },

    pay() {
      this.$router.push({
        path: "./pay", //跳转的路径
        query: {
          //路由传参时push和query搭配使用 ，作用时传递参数
          value: this.yhvalue,
          valueitem: this.yqvalue,
          original: this.original,
          reduction: this.reduction,
        },
      });
    },
    getgas() {
      // 加油页面接口
      getgass(this.id, this.lat, this.lng).then((res) => {
        if (res.status !== 200) {
          this.$message.error(res.data.msg);
        }
        if (res.data.status == 200) {
          this.oilnumber = res.data.data.oil_list;

          this.address = res.data.data.address;
          this.lat01 = res.data.data.lat;
          this.lng01 = res.data.data.lng;
          this.contact_mobile = res.data.data.mobile;
          this.business_hours = res.data.data.businessHours;
          this.lat = res.data.data.lat;
          this.lng = res.data.data.lng;
          this.logo = res.data.data.logo;
          this.ifRecharge = res.data.data.ifRecharge;
          this.shop_name = res.data.data.title;
          // this.yh = res.data.data.discount;
          this.distance = res.data.data.distance;
          this.rzid = res.data.data.id;
          this.mch_id = res.data.data.mch_id;
          this.yk = res.data.data.card_blg;

          for (var i = 0; i < this.oilnumber.length; i++) {
            this.oilgun = this.oilnumber[0].oil_gun;
            this.oilgunactive = res.data.data.oil_list[0].type;
            this.oilgunId = res.data.data.oil_list[0].id;
            this.reduction = this.oilnumber[0].price;
            this.showDiscount = this.oilnumber[0].showDiscount;
            this.oil_number = this.oilnumber[0].type;
            this.yqvalue = this.oilnumber[0].oil_gun[0].name;
            this.activeClass = this.oilnumber[0].id;
            this.yh = this.oilnumber[0].discountList;
          }
        } else {
          this.$message.error(res.data.msg);
          setTimeout(() => {
            this.$router.push({
              path: "/login", //跳转的路径
            });
          }, 1200);
        }
      });
    },
    // 加油优惠
    getsuggeprices() {
      getsuggeprice(this.id, 2, this.oilgunId).then((res) => {
        if (res.status !== 200) {
          this.$message.error(res.data.msg);
        }
        if (res.data.status == 607) {
          this.$message.error(res.data.msg);
        } else if (res.data.status == 608) {
          this.$message.error(res.data.msg);
          setTimeout(() => {
            this.$router.push({
              path: "/login", //跳转的路径
            });
          }, 1200);
        } else {
          this.movies = res.data.data;
        }
      });
    },
    // 加油金额
    getDiscounts() {
      getDiscount(
        Number(this.id),
        Number(this.price),
        2,
        String(this.oilgunId)
      ).then((res) => {
        if (res.status !== 200) {
          this.$message.error(res.data.msg);
        }
        if (res.data.status !== 200) {
          // this.$message.error(res.data.msg);
          setTimeout(() => {
            this.$router.push({
              path: "/login", //跳转的路径
            });
          }, 1200);
        } else {
          // this.dis_id_arr = res.data.data.dis_id_arr;
          this.Discount = res.data.data;
          this.have_discount = this.Discount.have_discount;
          window.localStorage.setItem("balance", res.data.data.balance);
          window.localStorage.setItem("balance_pay", res.data.data.balance_pay);
        }
      });
    },
    // 去加油
    jiayou() {
      this.drawer = true;
      this.getsuggeprices();
      this.getDiscounts();
      // this.price=this.movies[0].price
    },
    // 去支付
    orderConfirm() {
      if (this.price == 0) {
        this.$message.error("价格不能为0");
      } else {
        getOrderConfirm(
          this.id,
          2,
          this.activeClass,
          this.price,
          this.yqvalue,
          this.dis_id_arr
        ).then((res) => {
          if (res.status !== 200) {
            this.$message.error(res.data.msg);
          }
          if (res.data.code == 1) {
            this.$message.error(res.data.msg);
            setTimeout(() => {
              this.$router.push({
                path: "/login", //跳转的路径
              });
            }, 1200);
          } else {
            // this.$message.success(res.data.msg);
            setTimeout(() => {
              this.$router.push({
                path: "/pay", //跳转的路径
                query: {
                  //路由传参时push和query搭配使用 ，作用时传递参数
                  params: res.data.data,
                  price: this.price,
                  id: this.id,
                  activeClass: this.activeClass,
                  yqvalue: this.yqvalue,
                  dis_id_arr: this.dis_id_arr,
                },
              });
            }, 1000);
          }
        });
      }
    },
    // 输入框实时监听
    changeinput() {
      this.getsuggeprices();
      this.getDiscounts();
      for (var i = 0; i < this.movies.length; i++) {
        // if (this.price > this.movies[this.movies.length - 1].price) {
        //   this.priceitem = this.movies[this.movies.length - 1].price;
        // } else if (this.price >= this.movies[i].price) {
        //   this.priceitem = this.movies[i].price;
        // } else if (this.price < this.movies[0].price) {
        //   this.priceitem = "";
        // }
        if(this.price === this.movies[i].price){
           return this.priceitem = this.movies[i].price;
          
        }else{
           this.priceitem = ""
         
        }
      }
    },
    // 选择优惠
    changeyh(a) {
      this.getsuggeprices();
      this.priceitem = a;
      this.price = a;
      this.getDiscounts();
    },
    // 关闭弹窗恢复原始值
    closedrawer() {
      for (var i = 0; i < this.movies.length; i++) {
        this.price = "";
        this.priceitem = "";
      }
      this.getsuggeprices();
      this.getDiscounts();
    },
    // 地图
    space(lat, lng) {
      this.$router.push({
        path: "./map", //跳转的路径
        query: {
          wz: lat,
          wzw: lng,
        },
      });
    },

    //油卡充值
    card() {
      this.$router.push({
        path: "/cardlist",
        query: {
          id: this.id,
          rzid: this.rzid,
          mch_id: this.mch_id,
        },
      });
    },
  },
  components: {
    headerpu: headerpu,
    servicedialog: servicedialog,
    lubricate: lubricate,
  },
};
</script>
<style scoped>
.show-more {
  border: none;
  background: transparent;
  margin: 0 auto;
  display: block;
  color: var(--active);
}
.show-more i {
  text-align: center;
  display: block;
  font-size: 2.5rem;
  margin-top: 0.5rem;
}
/* .dis-content {
  height: 45px;
  overflow: hidden;
} */
/* .dis-content.actives {
  height: auto;
  overflow: auto;
  transition: all 0.4s;
} */
.button-styles {
  background: var(--white);
  color: var(--active);
  letter-spacing: 0.2rem;
  border-radius: 0.9rem;
  border: 1px solid var(--active);
  padding: 1.2rem;
  -webkit-box-shadow: 0rem 0.3rem 0.5rem #ffd8c3;
  box-shadow: 0rem 0.3rem 0.5rem #ffd8c3;
  margin-bottom: 0;
}
.jiayou-button {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  margin-bottom: 0 !important;
  border-radius: 0 !important;
  height: 50px;
}
.backicon {
  background: rgba(0, 0, 0, 0.5) !important;
}
>>> input::-webkit-input-placeholder {
  color: #c0c4cc;
  font-size: 1.5rem;
}

>>> input::-moz-input-placeholder {
  color: #c0c4cc;
  font-size: 1.5rem;
}

>>> input::-ms-input-placeholder {
  color: #c0c4cc;
  font-size: 1.5rem;
}
.space i {
  color: var(--active);
  background: #fba93b57;
  border-radius: 50%;
  padding: 0.2rem 0.2rem;
  font-size: 1.5rem;
  width: 3rem;
  height: 3rem;
  margin: 0 auto;
  text-align: center;
  line-height: 2.9rem;
}
.el-message-box__btns {
  display: flex;
}
.header {
  background: transparent !important;
  position: absolute !important;
  width: 80% !important;
}
.oilnumber,
.oil-gun {
  margin-bottom: 1rem;
}
.top-bg {
  height: 15.5rem;
  width: 100%;
  filter: blur(2px);
}
.topbg-style {
  background: var(--white);
  position: relative;
  -webkit-box-shadow: 0rem 0.5rem 0.5rem #e5e5e5;
  box-shadow: 0rem 0.5rem 0.5rem #e5e5e5;
  border-radius: 1.2rem;
  margin-top: -10rem;
  z-index: 555;
}
.top-content {
  padding: 1.8rem 1.5rem 1.8rem 1.8rem;
}
.topimg1 {
  width: 8.5rem;
  height: 8.5rem;
  border-radius: 10px;
}
.title {
  font-size: 1.6rem;
  font-family: Source Han Sans SC;
  font-weight: bold;
  color: #000000;
  margin-left: 2.5rem;
  margin-bottom: 0rem;
  display: inline-block;
  /* margin-top: 0.5rem; */
}
.date {
  position: relative;
}
.msg > div {
  font-size: 1.1rem;
  font-family: Source Han Sans SC;
  font-weight: 400;
  color: #000000;
  margin-left: 2.5rem;
  display: flex;
  /* margin-bottom: 0.5rem; */
}
.msg div img {
  color: #ed6f28;
  font-size: 1.3rem;
  margin-right: 5px;
  width: 15px;
  height: 15px;
  padding-top: 2px;
}
.msg div div {
  flex: 1;
}
.map {
  margin-left: 0 !important;
  text-align: right;
  position: absolute;
  right: -0.6rem;
  top: 0;
}
/* .discount {
  font-size: 1.1rem;
  font-family: Source Han Sans SC;
  font-weight: 400;
  color: var(--active);
  line-height: 1.8rem;
  margin-top: 0.5rem;
}
.discount .dis-title {
  color: #000;
  margin-bottom: 0.5rem;
}
.discount p {
  margin-bottom: 0.2rem;
  display: flex;
  
}
.discount span {
  display: inline-block;
  width: 6rem;
}
.discount i {
  width: calc(100% - 6rem);
}
.discount span a {
  padding: 0.2rem 0.5rem;
  border: 1px solid var(--active);
  border-radius: 0.2rem;
  font-size: 1rem;
  display: inline-block;
  line-height: 1.2rem;
  transform: scale(0.9);
  color: var(--active);
} */
.discount {
  padding: 10px 3.5%;
}
.discount .dis-title {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 5px;
}
.discount-box1 .activity-number {
  color: #000;
  font-weight: 700;
  font-size: 2rem;
}
.discount-box1 .price-reduction {
  color: #ccc;
  font-weight: 500;
  font-size: 1.5rem;
}
.discount-box2 .activity-desc {
  font-weight: 500;
  color: #db261b;
  font-size: 1.25rem;
}
.discount-box2 .activity-icon {
  font-weight: 700;
  color: #db261b;
  font-size: 1.5rem;
}
.discount-box2 .activity-price {
  font-weight: 700;
  color: #db261b;
  font-size: 2.75rem;
}
.activity-box {
  background: #f1f1f1;
  border-radius: 5px;
  padding: 10px;
}
.activity-box .activity-item {
  display: flex;
  margin-bottom: 10px;
}
.activity-box .activity-name {
  width: 6rem;
  background: linear-gradient(90deg, #db261b 0%, #fa8440 100%);
  border-radius: 5px 0px 0px 5px;
  color: #fff;
  text-align: center;
  font-weight: 700;
  font-size: 1.25rem;
  display: flex;
  align-items: center;
  justify-content: center;
}
.activity-box .activity-content {
  flex: 1;
  line-height: 25px;
}
.activity-box .activity-content span {
  border-radius: 0px 5px 5px 0px;
  font-size: 1.25rem;
  color: #db261b;
  padding: 0 10px;
  border: 1px solid #ee7325;
  background-color: #fff;
  display: inline-block;
  word-break: break-all;
}
.activity-box .activity-item:last-child {
  margin-bottom: 0;
}

.number-header {
  margin-top: 0rem;
}
.number-header h4,
.gun-header h4 {
  font-size: 1.6rem;
  font-family: Source Han Sans SC;
  font-weight: 500;
  color: #000000;
}
.original-price {
  font-size: 0.5rem;
  font-family: Source Han Sans SC;
  font-weight: 800;
  text-decoration: line-through;
  color: #999999;
}

.number-content-item {
  border: 1px solid var(--active);
  color: var(--active);
  /* padding: 0.6rem 2.2rem; */
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22%;
  border-radius: 0.9rem;
  font-weight: bold;
  font-size: 1.9rem;
  height: 40px;
  box-sizing: border-box;
  margin: 1rem 4% 1rem 0;
  /* margin: 1rem 2rem 1rem 0; */
}
.number-content-item:nth-child(4n) {
  margin-right: 0;
}
.active {
  background: var(--active);
  color: var(--white);
  box-shadow: 0rem 0.3rem 0.5rem #ffd8c3;
}
.help {
  font-size: 1.1rem;
  font-family: Source Han Sans SC;
  font-weight: 400;
  color: #999999;
  margin-bottom: 50px;
  text-align: center;
  margin-top: 1rem;
  /* width: 100%; */
  border: 0.1rem dashed var(--active);
  padding: 0.1rem;
  border-radius: 0.5rem;
  height: 1.5rem;
  line-height: 1.5rem;
}
.submitbtn button {
  margin-bottom: 8.8rem;
  width: 100%;
  background: var(--active);
  color: var(--white);
  font-size: 1.4rem;
  letter-spacing: 0.2rem;
  border-radius: 0.9rem;
  border: none;
  padding: 1.2rem;
  box-shadow: 0rem 0.3rem 0.5rem #ffd8c3;
}
.meaasgeicon {
  color: red;
  font-size: 2rem;
  margin: 0 auto;
  text-align: center;
  display: block;
}
.dialog-footer {
  display: flex;
}
.dialog-footer button {
  width: 50%;
}
@media screen and (min-width: 475px) {
  .topimg1 {
    width: 9rem;
  }
}
/* .drawer {
  padding-bottom: 4.5rem;
} */
.drawer .marginstyle {
  margin: 0 0.4rem;
}
.drawer .title {
  font-size: 3.5rem;
  font-family: Source Han Sans SC;
  font-weight: 800;
  color: #000000;
  -webkit-box-align: baseline;
  -ms-flex-align: baseline;
  align-items: baseline;
  margin-top: -14px;
  margin-bottom: 1.5rem;
}
.drawer .title sub {
  font-size: 2rem;
}
.drawer .lubricate-item {
  width: 8rem;
  height: 6rem;
  border: 0.15rem solid var(--active);
  border-radius: 1rem;
  -webkit-box-flex: 1;
  -ms-flex: 1;
  flex: 1;
  margin: 1rem 0.4rem;
  text-align: center;
  line-height: 2rem;
  font-weight: bold;
}

.drawer >>> .el-input__inner {
  border: 1px solid var(--grey);
  border-radius: 10px;
  text-align: center;
  color: #000;
  height: unset;
  padding: 22px 15px;
  font-size: 3rem;
}

.drawer .lubricate-item p:nth-child(1) span {
  margin-top: 1rem;
  display: inline-block;
  font-size: 2.2rem;
}

.drawer .sub {
  font-size: 1.6rem;
}
.drawer .lubricate-item p:nth-child(2) {
  margin-top: 0.2rem;
  color: var(--active);
  font-size: 1rem;
}
.total-discount {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: 10px 0;
  padding-bottom: 10px;
  border-bottom: 1px solid #e9e9e9;
}
.no-discount {
  padding: 0;
  border: 0;
  font-size: 1.3rem;
  margin: 20px 0;
}
.total-discount .discount-title {
  color: #000;
  font-weight: 700;
  font-size: 1.5rem;
}
.total-discount p {
  font-size: 1.3rem;
}

.drawer .lubricate-item1 {
  font-size: 1.3rem;
  color: var(--active);
  margin-bottom: 10px;
}
.drawer .lubricate-item1 i {
  font-size: 2rem;
  color: #999;
  margin-top: 0.2rem;
}
.drawer .el-footer {
  position: fixed;
  bottom: 0;
  width: 100%;
  padding: 0;
  border-top: 1px solid #d1d1d1;
  height: unset !important;
  
}

.drawer .lubricate-item a {
  color: #000000;
}
.drawer .lubricate-item.active {
  background: var(--active);
}
.drawer .lubricate-item.active a,
.lubricate-item.active p:nth-child(2) {
  color: #fff;
}
.mt {
  margin-top: 1.5rem;
}
.text-color {
  color: #ccc !important;
}
.drawer >>> .el-drawer.btt {
  margin-bottom: 4.5rem;
}
.drawer >>> .el-drawer__close-btn {
  padding: 0;
}
.drawer >>> .el-drawer__header {
  margin-bottom: 30px;
}
.drawer >>> .el-main {
  padding-top: 0;
}
.drawer >>> .marginstyle {
  margin: 0;
  margin-bottom: 1rem;
}

.footer-button{
  display: flex;
  height: 4.5rem;
}
.drawer .lubricate-item2 {
  flex: 1;
  padding:0 10px 0 20px ;
  background-color: #fff;

}

.drawer .lubricate-item2 p:nth-child(2) {
  font-size: 1.3rem;
  color: #999;
}
.drawer .lubricate-item2 .newprice {
  font-weight: 700;
  color: #db261b;
  font-size: 2rem;
  margin-right: 5px;
}
.drawer .lubricate-item2 .oldprice {
  font-weight: 500;
  text-decoration: line-through;
  color: #666666;
  font-size: 1.25rem;
}
.footer-button .footer-submit{
  height: unset;
  width: 10rem;
  border-radius: unset;
  margin: 0;
  padding: 0;
}
.footer-icon{
  color:#db261b;
  font-size:1.5rem;
  font-weight: 600;
}
</style>
