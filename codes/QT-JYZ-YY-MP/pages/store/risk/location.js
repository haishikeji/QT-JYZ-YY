// pages/store/risk/location.js
const App = getApp();

Page({

  /**
   * 页面的初始数据
   */
  data: {
    location: false,
    range: ''
  },
  //切换定位开启关闭
  switchChange(e) {
    let location = e.detail.value;
    this.setData({
      location
    })
    if (!location) {
      this.saveLocation()
    }
  },
  // 获取输入框中的值
  bindKeyInput(e) {

    let range = e.detail.value.replace(/[^\d]/g,'');
    this.setData({
      range
    })
  },
  //保存
  save() {
    let _this = this;
    let {range} = _this.data;
    if (range == '') {
      wx.showToast({
        title: '距离不能为空',
        icon: 'none',
        duration: 2000
      })
      return false
    }
    this.saveLocation()
  },

  saveLocation(){
    let _this = this;
    let {range} = _this.data;

    App._get1('store/saveLocation', {
      storeId: _this.data.storeId,
      distance:range,
      locationStatus:_this.data.location
    }, res => {
      if (res.status === 200) {
        wx.showToast({
          title: '修改成功',
          icon:"success"
        })
        setTimeout(function () {
          wx.navigateBack({
            delta: 1
          })
        }, 1000);
      }else{
        wx.showToast({
          title: res.msg,
          icon:"none"
        })
      }
    })
  },
  /**
   * 生命周期函数--监听页面加载
   */
  onLoad(options) {
    this.setData({
      storeId: options.storeId
    })
  },
  getData() {
    let _this = this;
    App._get1('store/eidtLocation', {
      storeId: _this.data.storeId,
    }, res => {
      _this.setData({
        location: res.data.locationStatus ? true : false,
        range:res.data.distance
      })
    })
  },

  /**
   * 生命周期函数--监听页面初次渲染完成
   */
  onReady() {

  },

  /**
   * 生命周期函数--监听页面显示
   */
  onShow() {
    this.getData()
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