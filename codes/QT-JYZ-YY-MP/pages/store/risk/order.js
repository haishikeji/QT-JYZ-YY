// pages/store/risk/totalDetail.js
const App = getApp();
var time = require('../../../utils/util.js');

Page({

  /**
   * 页面的初始数据
   */
  data: {
    name: '',
    phone: '',
    is_no_more: 0,
    is_loading: 0,
    listData: [],
    page: 1,
    size: 20,
    flag:true,
    height:'',
    realName:''
  },
 
  onLoad(options) {
    this.setData({
      storeId: options.storeId,
      userId: options.id,
      startTime:options.startTime,
      endTime:options.endTime
    })
  },

  onShow() {
    this.getData(1);
    this.setData({
      height: this.data.flag ? '232rpx' : '184rpx'
    })
  },
  refresherrefresh(e) {
    this.setData({
      page: 1,
      is_no_more: 0,
      is_loading: 0,
    })
    this.getData(1)
  },
  Reachbottom(e) {
    let _this = this;
    if (e.detail.direction === "bottom") {

      if (_this.data.is_loading == 0 && _this.data.is_no_more == 0) {
        _this.setData({
          is_loading: 1,
          page: ++_this.data.page
        })
        _this.getData(_this.data.page)
      }
    }
  },
  getData(page) {
    if (page === 1) {
      this.setData({
        listData: []
      })
    }
    let _this = this;
    App._get1('risk/orderList', {
      storeId: _this.data.storeId,
      userId: _this.data.userId,
      beginDate: _this.data.startTime,
      endDate: _this.data.endTime,
      page,
      limit: _this.data.size
    }, res => {
      let {list} = res.data;
      if (page == 1 && list && list.length > 0) {
        _this.setData({
          name: list[0].userName,
          phone: list[0].mobile
        })
      }
      for (var i = 0; i < list.length; i++) {
        list[i].createTime = time.formatTimeTwo(list[i].createTime, 'Y-M-D h:m:s');
      }
      let listData = [..._this.data.listData, ...list];
      let realName = res.data.realName;
      let flag = res.data.realName=='' ? false:true;
      _this.setData({
        is_no_more: listData.length >= res.data.count ? 1 : 0,
        is_loading: listData.length >= res.data.count ? 1 : 0,
        listData,
        realName,
        flag
      })
    })

  },
  /**
   * 生命周期函数--监听页面隐藏
   */
  onHide() {

  },

  /**
   * 生命周期函数--监听页面卸载
   */
  onUnload() {

  },

  /**
   * 页面相关事件处理函数--监听用户下拉动作
   */
  onPullDownRefresh() {

  },

  /**
   * 页面上拉触底事件的处理函数
   */
  onReachBottom() {

  },

  /**
   * 用户点击右上角分享
   */
  onShareAppMessage() {

  }
})