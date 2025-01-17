<template>
  <div class="list mt55" style="height: 100%; width: 100%">
    <headerpu  > <i class="el-icon-arrow-left backicon" slot="back"></i> <div class="grid-content bg-purple-dark" slot="title">地图洗车</div></headerpu>
    <el-scrollbar class="mb58" style="height: 100%"
      ><div class="infinite-list-wrapper" style="overflow: auto; height: 85vh">
        <ul
          class="list padding-style"
          v-infinite-scroll="load"
          infinite-scroll-disabled="disabled"
          infinite-scroll-immediate="true"
          infinite-scroll-distance="50"
        >
          <li 
            v-for="(item,index,wzj,wzw) in list"
           :key="index"
            class="infinite-list-item flex flex-y-center list-item"
          >
            <div class="main flex1 flex-y-center">
              <div class="img">
                <el-avatar
                  shape="square"
                  :size="50"
                  :src="item.img"
                ></el-avatar>
              </div>
              <div class="value">
                <h4 class="title">{{ item.title }}</h4>
                <p class="money">￥{{ item.money }}</p>
                <p class="address">
                  <i class="el-icon-location"></i> {{ item.address }}
                </p>
              </div>
            </div>
            <div class="space" :id="index" :data-wz="wzj" :data-wzw="wzw"  @click="space(index,$event,item.wzj,item.wzw)">
              <i class="el-icon-position" style="cursor: pointer;"></i>
              <p>{{ item.space }}m</p>
            </div>
          </li>
        </ul>
        <p v-if="loading" v-loading="loadingb" class="text-center">加载中...</p>
        <p v-if="noMore" class="text-center" data-aos-duration="10">没有更多了</p>
      </div>
    </el-scrollbar>
    <tabbar></tabbar>
  </div>
</template>

<script>

import headerpu from '@/components/commont/header'
import tabbar from "@/components/commont/tabbar";
export default {
  name: "list",
  data() {
    return {
      count: 0, //起始页数值为0
      loading: false,
      loadingb: false,
      changeaos:'zoom-in',
      flag:false,
      totalPages: "", //取后端返回内容的总页数
      list: [], //后端返回的数组
      lists: [
        {
           img: require("../../assets/images/jyimg.jpg"),
          title: "台州市城关路239号",
          money: "20",
          address: "福建省厦门市湖里区汤屿路",
          space: "100",
          wzj:24.515265,
          wzw:118.175468
      
        },
        {
           img: require("../../assets/images/jyimg.jpg"),
          title: "经十路伯乐汽车美容院",
          money: "20",
          address: "福建省厦门市同安区",
          space: "100",
           wzj:24.678393,
          wzw:118.175447
          
        },
        {
           img: require("../../assets/images/jyimg.jpg"),
          title: "经十路伯乐汽车美容院",
          money: "20",
          address: "济南市历下区01号",
          space: "100",
           wzj:24.480900,
          wzw:118.098907
           
        },
        {
           img: require("../../assets/images/jyimg.jpg"),
          title: "经十路伯乐汽车美容院",
          money: "20",
          address: "福建省厦门市湖里区长浩东路19-107号",
          space: "100",
           wzj:24.480900,
          wzw:118.098907
        },
        {
           img: require("../../assets/images/jyimg.jpg"),
          title: "经十路伯乐汽车美容院",
          money: "20",
          address: "济南市历下区01号",
          space: "100",
           wzj:24.521513,
          wzw:118.109207
        },
        {
          img: "../assets/logo.png",
          title: "经十路伯乐汽车美容院",
          money: "20",
          address: "福建省厦门市思明区湖滨北路",
          space: "100",
           wzj:24.480900,
          wzw:118.098907
        },
      ],
    };
  },
  computed: {
    noMore() {
      //当起始页数大于总页数时停止加载
      return this.count >= this.totalPages - 1;
      this.loadingb = false;
    },
    disabled() {
      // return this.loading = false;
      return this.loading || this.noMore;
    },
  },
  created() {
    this.getMessage();
    this.loading = false;
    this.loadingb = false;
      this.changeaos="";

  },
  methods: {
    load() {
      //滑到底部时进行加载
      this.loading = true;
      this.loadingb = true;
      this.changeaos=""
      setTimeout(() => {
        this.count += 1; //页数+1
        this.getMessage(); //调用接口，此时页数+1，查询下一页数据
      }, 2000);
    },
    getMessage() {
      let params = {
        pageNumber: this.count,
        pageSize: 2, //每页查询条数
      };
      //   this.$axios
      //     .post(
      //       "https://接口",
      //       params
      //     )
      //     .then(res => {
      this.list = this.list.concat(this.lists); //因为每次后端返回的都是数组，所以这边把数组拼接到一起
      this.totalPages = 3;
      this.loading = false;
      this.loadingb = false;
      //     })
      //     .catch(err => {
      //     });
    },

    space(a,$event,wzj,wzw){
       if($event.currentTarget.id==a){
       this.$router.push({ 
        path:'./map',   //跳转的路径
        query:{           //路由传参时push和query搭配使用 ，作用时传递参数
          id:$event.currentTarget.id , 
          wz:wzj,
          wzw:wzw
        }
      })
       }
    }
  },
  components: {
    headerpu: headerpu,
    tabbar:tabbar
  },
};
</script>
<style scoped>
.header {
  background:  var(--white);
}

.list-item {
  margin-bottom: 3rem;
}
.img {
width: 5.5rem;
    height: 5.5rem;
    margin-right: 1.5rem;
}
.img img {
  width: 100%;
  height: 100%;
}
.title {
  font-weight: bold;
  font-size: 1.3rem;
  margin-bottom: 0.5rem;
}
.money {
  color: red;
  margin-bottom: 0.4rem;
  font-size: 1.3rem;
}
.address {
  font-size: 1.2rem;
  color: #666666ab;
}
.space i {
  color: #27b148;
  background: #ddffe5;
  border-radius: 50%;
  padding: 1rem 1.1rem;
}
.space p {
  text-align: center;
  font-size: 1.2rem;
  color: #666666ab;
  margin-top: 0.3rem;
}
</style>

