export interface Waybill {
  id: string
  waybillNo: string
  orderNo: string
  carrierName: string
  trackingNo: string
  status: 'pending' | 'picked' | 'in_transit' | 'delivered' | 'exception'
  statusLabel: string
  senderName: string
  senderAddress: string
  receiverName: string
  receiverAddress: string
  createdAt: string
}

export interface TrackingEvent {
  id: string
  time: string
  location: string
  description: string
  status: string
}
