package com.example.snitchsms.recieptdata

import android.content.Context
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.snitchsms.R
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.contacts.smsreceiver.SmsReceiver
import de.hdodenhof.circleimageview.CircleImageView

class ReceiptAdapter(
    private val mContext: Context,
    private val items: List<SmsSaveModel>,
    val callBack: ClickCallback,
    private val contactPhotoList: Map<String, Bitmap>,
    private val unreadCounts: MutableMap<String, Int>
) : RecyclerView.Adapter<ReceiptAdapter.ViewHolder>() {

    private var sharedPreferences: SharedPreferences? = null
    private var filteredList: List<SmsSaveModel> = items
    private var selectedItems = mutableListOf<SmsSaveModel>()

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textPhoneNumber: TextView = itemView.findViewById(R.id.textPhoneNumber)
        val name: TextView = itemView.findViewById(R.id.name)
        val txtUnreadCounts: TextView = itemView.findViewById(R.id.unreadCountTextView)
        val cardCounter: CardView = itemView.findViewById(R.id.cardCounter)
        val incomingMessage: TextView = itemView.findViewById(R.id.incomingMessage)
        val mImg: CircleImageView = itemView.findViewById(R.id.mImg)
        val delete_checkbox: CheckBox = itemView.findViewById(R.id.delete_checkbox)
    }

    init {
        // Initialize SharedPreferences once in the constructor
        sharedPreferences = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    }

    fun filterList(query: String) {
        // Filter the data based on the query
        filteredList = items.filter {
            it.receiptName.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }
    fun updateData(newData: List<SmsSaveModel>) {
        filteredList = newData
        notifyDataSetChanged()
    }
    fun getSelectedItems(): List<SmsSaveModel> {
        return selectedItems
    }
    fun toggleItemSelection(position: Int) {
        val item = filteredList[position]
        if (selectedItems.contains(item)) {
            callBack.updateDeleteButtonVisibility()
        } else {

        }
        notifyItemChanged(position)
//        callBack.updateDeleteButtonVisibility()

    }
    fun updateUnreadCount(contactName: String, newUnreadCount: Int) {
        Log.d(
            "unreadMessagesReceiver",
            "adapter Updating unread count for $contactName to $newUnreadCount"
        )

        unreadCounts[contactName] = newUnreadCount
        notifyDataSetChanged()

        // Save the updated unread count to SharedPreferences
        saveUnreadCountToSharedPreferences(contactName, newUnreadCount)
    }

    private fun saveUnreadCountToSharedPreferences(contactName: String, unreadCount: Int) {
        val editor = sharedPreferences!!.edit()
        editor.putInt(contactName, unreadCount)
        editor.apply()

        // Log to verify if the value is being saved correctly
        Log.d("unreadMessagesReceiver", "Saved unread count for $contactName: $unreadCount")
    }

    private fun getUnreadCountFromSharedPreferences(contactName: String): Int {
        // Retrieve the unread count from SharedPreferences
        val unreadCount = sharedPreferences!!.getInt(contactName, 0)

        // Log to verify if the value is being retrieved correctly
        Log.d("unreadMessagesReceiver", "Retrieved unread count for $contactName: $unreadCount")

        return unreadCount
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = filteredList[position]
        holder.textPhoneNumber.text = item.phoneNumber
        holder.name.text = item.receiptName
        val phoneNumber = item.phoneNumber
        val contactPhoto = contactPhotoList[phoneNumber]

        Log.d("ReceiptAdapter", "Phone Number: $phoneNumber, Contact Photo: $contactPhoto")

        // Retrieve the unread count from SharedPreferences
        val unreadCount = getUnreadCountFromSharedPreferences(item.receiptName)
        if (unreadCount.equals(0)) {
            holder.txtUnreadCounts.visibility = View.GONE
            holder.cardCounter.visibility = View.GONE
        } else {
            holder.txtUnreadCounts.visibility = View.VISIBLE
            holder.cardCounter.visibility = View.VISIBLE
            holder.txtUnreadCounts.text = unreadCount.toString()
        }

        if (contactPhoto != null) {
            Log.d("abdphoto", "onBindViewHolder:  null ni ha $contactPhoto")
            holder.mImg.setImageBitmap(contactPhoto)
        } else {
            holder.mImg.setImageResource(R.drawable.ic_contacts_complete)
            Log.d("abdphoto", "onBindViewHolder:  null ha")

        }


        holder.delete_checkbox.isChecked = selectedItems.contains(item)
        holder.delete_checkbox.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                selectedItems.add(item)
            } else {
                selectedItems.remove(item)
            }
        }

        holder.itemView.setOnLongClickListener {
            val backgroundColor =
                 ContextCompat.getColor(mContext, R.color.blue)

            holder.itemView.setBackgroundColor(backgroundColor)
            holder.delete_checkbox.visibility = View.VISIBLE
            holder.delete_checkbox.isChecked = !holder.delete_checkbox.isChecked

            toggleItemSelection(position)

            true

        }

        holder.itemView.setOnClickListener {
            callBack.itemClick(items.indexOf(item))
        }

//        val lastReceivedMessage = SmsReceiver.getLastReceivedMessage(item.receiptName ?: item.phoneNumber)
        val lastReceivedMessage =
            SmsReceiver.getLastReceivedMessage(mContext, item.receiptName ?: item.phoneNumber)

        holder.incomingMessage.text = lastReceivedMessage ?: ""

    }

    override fun getItemCount(): Int = filteredList.size
}

/*class ReceiptAdapter(
    private val mContext: Context,
    private val items: List<SmsSaveModel>,
    val callBack: ClickCallback,
    private val contactPhotoList: Map<String, Bitmap>,
    private val unreadCounts: MutableMap<String, Int>
) :
    RecyclerView.Adapter<ReceiptAdapter.ViewHolder>() {
    private var sharedPreferences :SharedPreferences?=null

    private var filteredList: List<SmsSaveModel> = items


    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textPhoneNumber: TextView = itemView.findViewById(R.id.textPhoneNumber)
        val name: TextView = itemView.findViewById(R.id.name)
        val txtUnreadCounts: TextView = itemView.findViewById(R.id.unreadCountTextView)
        val mImg: CircleImageView = itemView.findViewById(R.id.mImg)
    }


    fun filterList(query: String) {
        // Filter the data based on the query
        filteredList = items.filter {
            it.receiptName.contains(query, ignoreCase = true)
        }
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_receipt, parent, false)
        return ViewHolder(view)
    }

    fun updateUnreadCount(contactName: String, newUnreadCount: Int) {
        Log.d(
            "unreadMessagesReceiver",
            "adapter Updating unread count for $contactName to $newUnreadCount"
        )

            unreadCounts[contactName] = newUnreadCount
            notifyDataSetChanged()

    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = items[position]
        val item = filteredList[position]
        holder.textPhoneNumber.text = item.phoneNumber
        holder.name.text = item.receiptName

        val phoneNumber = item.phoneNumber
        val contactPhoto = contactPhotoList[phoneNumber]

        Log.d("ReceiptAdapter", "Phone Number: $phoneNumber, Contact Photo: $contactPhoto")


        val unreadCount = unreadCounts[item.receiptName] ?: 0
        sharedPreferences = mContext.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        Log.d("unreadMessagesReceiver", "adapter: unreadCount $unreadCount ")
        val editor = sharedPreferences!!.edit()
        editor.putInt("unreadCount", unreadCount)
        editor.apply()
        val unreadCountget = sharedPreferences!!.getInt("unreadCount", 0)
        Log.d("unreadMessagesReceiver", "adapter: unreadCountget $unreadCountget ")

        holder.txtUnreadCounts.text = unreadCountget.toString()

        if (contactPhoto != null) {
            holder.mImg.setImageBitmap(contactPhoto)
        } else {
            holder.mImg.setImageResource(R.drawable.ic_contacts_complete)

        }
        holder.itemView.setOnClickListener {
//            callBack.itemClick(position)
            callBack.itemClick(items.indexOf(item))
        }
    }

    override fun getItemCount(): Int = filteredList.size
}*/
