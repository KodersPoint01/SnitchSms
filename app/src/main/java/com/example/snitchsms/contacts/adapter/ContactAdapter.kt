package com.example.snitchsms.contacts.adapter


import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.recyclerview.widget.RecyclerView
import com.example.snitchsms.R
import com.numbertracker.phonelocator.locationfinder.calllogs.DialerCallBack
import com.example.snitchsms.contacts.model.ContactModel


class ContactAdapter(
    private var contactList: ArrayList<ContactModel>,
    val context: Context,
    val callback: DialerCallBack
) : RecyclerView.Adapter<ContactAdapter.ViewHolder>() {
    override fun onCreateViewHolder(p0: ViewGroup, p1: Int): ViewHolder {

        return ViewHolder(
            LayoutInflater.from(p0.context)
                .inflate(R.layout.item_design_contact, p0, false) as View
        )
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val data = contactList[position]

        holder.tvName.text = data.contactName
        holder.tvNumber.text = data.number

        holder.mainitem.setOnClickListener {
            callback.setIntent(data)
        }

    }

    override fun getItemCount(): Int {
        return contactList.size
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val tvName: TextView = view.findViewById(R.id.tvContactName)
        val tvNumber: TextView = view.findViewById(R.id.tvContactNumber)
        val mainitem: ConstraintLayout = view.findViewById(R.id.mainitem)


    }

    fun filterList(filterList: ArrayList<ContactModel>) {
        contactList = filterList
        notifyDataSetChanged()
    }
}