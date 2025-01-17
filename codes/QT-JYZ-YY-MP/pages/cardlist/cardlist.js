const App = getApp();
Page({

  /**
   * 页面的初始数据
   */
  data: {
    show: false,
    duration: 300,
    round: false,
    overlay: true,
    list: [],
    mch_id: 0,
    store_id: 0,
    cardList: {},
    activeindex: null,
    price: "",
    recommend: []
  },

  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    const mch_id = options.mch_id
    const store_id = options.store_id
    this.setData({
      mch_id,
      store_id
    })
    this.getmyCard()


  },
  // 获取油卡信息
  getmyCard() {
    App._get1('mch/myCard', {
      platform_id: this.data.mch_id
    }, result => {
      this.setData({
        cardList: result.data,
        list:result.data.list,
      });
    });
  },
  // 领取油卡
  receiveOil(){
    App._get1('mch/getMemberCard', {
      store_id: this.data.store_id,
      platform_id:this.data.mch_id
    }, result => {
      this.getmyCard()
    })
  },
  // 获取推荐金额
  suggestPrice() {
    App._get1('store/suggestPrice', {
      shop_id: this.data.store_id,
      type: 1
    }, result => {
      this.setData({
        recommend: result.data
      });

    });
  },
  // 获取优惠数据
  getDiscount(price_input) {
    App._get1('store/getDiscount', {
      shop_id: this.data.store_id,
      type: 1,
      price_input: price_input
    }, result => {
      this.setData({
        list: result.data
      });
    });
  },
  // 显示弹窗
  popup(e) {
    this.setData({
      show: true
    })
    this.suggestPrice();
  },
  // 关闭弹窗
  popupClose() {
    this.setData({
      show: false,
      price: "",
      activeindex: null
    })
  },

  // 获取点击的数据
  itemclick(e) {
    const {
      activeindex
    } = e.currentTarget.dataset
    const {
      price
    } = e.currentTarget.dataset
    this.setData({
      activeindex,
      price
    })
    if (price !== "") {
      this.getDiscount(price)
    }
  },
  // 获取输入框中的值
  bindKeyInput(e) {
    let price = e.detail.value.replace(/^\D/g,'').replace(/^(\-)*(\d+)\.(\d\d).*$/,'$1$2.$3').replace('.','$#$').replace(/\./g,'').replace('$#$','.');
    this.setData({
      price
    })
    if (price !== "") {
      this.getDiscount(price)
    }
  },
  // 离开弹窗触发
  onAfterLeave() {
    this.setData({
      price: "",
      activeindex: null
    })
  },
  // 跳转到订单结算页面
  // const   store_id=8
  //     const type=2
  //     const oil_id=3
  //     const  price=0 
  //     const  gun=2
  //     const  card_id=10
  goOrderView() {
    const _this = this
    const price = this.data.price
    if (_this.data.price > 0) {
      _this.getDiscount(0)
      _this.setData({
        price:"",
        activeindex:null
      },() => {
         wx.navigateTo({
        url: '../order/orderview?store_id=' + _this.data.store_id + '&type=' + 1 + '&oil_id=' + 0+ '&price=' + price + '&gun=' + 0 + '&card_id=' + _this.data.cardList.id
      })
      })
    // if (this.data.price > 0) {
    //   wx.navigateTo({
    //     url: '../order/orderview?store_id=' + this.data.store_id + '&type=' + 1 + '&oil_id=' + 0 + '&price=' + this.data.price + '&gun=' + 0 + '&card_id=' + this.data.cardList.id
    //   })
    }else{
      wx.showToast({
        title: '输入金额需大于0',
        icon: 'none',
        duration: 2000
      })
    }

  },
  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady: function () {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow: function () {

  },

  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide: function () {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload: function () {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh: function () {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom: function () {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage: function () {

  }
})