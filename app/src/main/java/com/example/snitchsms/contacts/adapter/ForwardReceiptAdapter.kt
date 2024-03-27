package com.example.snitchsms.contacts.adapter

import android.graphics.Bitmap
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.snitchsms.R
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.recieptdata.ClickCallback
import de.hdodenhof.circleimageview.CircleImageView

class ForwardReceiptAdapter(
    private val items: List<SmsSaveModel>,
    val callBack: ClickCallback,
    private val contactPhotoList: Map<String, Bitmap>,
    private val unreadCounts: Map<String, Int>
) :
    RecyclerView.Adapter<ForwardReceiptAdapter.ViewHolder>() {

    private var filteredList: List<SmsSaveModel> = items

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val textPhoneNumber: TextView = itemView.findViewById(R.id.textPhoneNumber)
        val name: TextView = itemView.findViewById(R.id.name)
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
            .inflate(R.layout.item_forward_receipt, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
//        val item = items[position]
        val item = filteredList[position]
        holder.textPhoneNumber.text = item.phoneNumber
        holder.name.text = item.receiptName

        val phoneNumber = item.phoneNumber
        val contactPhoto = contactPhotoList[phoneNumber]

        Log.d("ReceiptAdapter", "Phone Number: $phoneNumber, Contact Photo: $contactPhoto")


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

    override fun getItemCount(): Int = filteredList.size}
