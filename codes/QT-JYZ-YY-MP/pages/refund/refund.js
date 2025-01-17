// pages/refund/refund.js
let App = getApp();

Page({

  /**
   * 页面的初始数据
   */
  data: {
    order_id: null,
    order: {},
    show: false,
    duration: 0,
    round: false,
    overlay: false,
    items: [],
    reason: "",
    price: "",
    text: ""
  },
  // 获取输入框退款金额
  inputPrice(e) {

    let price = Number(price)
    price = e.detail.value.replace(/^\D/g, '').replace(/^(\-)*(\d+)\.(\d\d).*$/, '$1$2.$3').replace('.', '$#$').replace(/\./g, '').replace('$#$', '.');
    if (price > this.data.order.accounts_price) {
      price = this.data.order.accounts_price
    }
    this.setData({
      price
    })
  },
  // 获取输入退款说明
  inputExplain(e) {
    let text = e.detail.value
    this.setData({
      text
    })
  },
  // 显示弹窗
  popup(e) {
    this.setData({
      show: true,
      overlay: true
    });
  },
  // 关闭弹窗
  popupClose() {
    this.setData({
      show: false,
      overlay: false
    })
  },
  /**
   * 生命周期函数--监听页面加载
   */
  onLoad: function (options) {
    const order_id = options.order_id;
    this.setData({
      order_id
    })
    this.getOrderDetail(order_id);
    this.getRefundCause();
  },
  /**
   * 获取订单详情
   */
  getOrderDetail: function (order_id) {
    let _this = this;
    App._get1('order/orderDetail', {
      id: order_id
    }, function (result) {
      const order = result.data
      const price = order.accounts_price
      _this.setData({
        order,
        price
      });
    });
  },
  // 获取退款原因
  getRefundCause: function () {
    let _this = this;
    App._get1('order/getReturnRemark', {}, function (result) {
      const items = result.data
      _this.setData({
        items
      })
    });
  },
  // 更改退款原因
  radioChange(e) {
    const reason = e.detail.value
    this.setData({
      reason,
    })
  },
  // 提交退款申请
  submit: function () {
    let _this = this
    if (!_this.data.price || _this.data.price <= 0) {
      wx.showToast({
        title: '请填写退款金额且大于0',
        icon: 'none',
        duration: 2000
      })
      return
    }
    if (!_this.data.reason) {
      wx.showToast({
        title: '请选择退款原因',
        icon: 'none',
        duration: 2000
      })
      return
    }

    App._get1('order/refund', {
      id: _this.data.order_id,
      returnAmount: _this.data.price,
      remark: _this.data.reason,
      returnDesc: _this.data.text
    }, function (result) {
      if (result.status == 200) {
        wx.showToast({
          title: '提交审核成功',
          icon: 'none',
          duration: 2000
        })
        setTimeout(() => {
          wx.redirectTo({
            url: '/pages/refund/progress?order_id=' + _this.data.order_id + '&id=' + result.data,
          });
        }, 1000)
      }
    })
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