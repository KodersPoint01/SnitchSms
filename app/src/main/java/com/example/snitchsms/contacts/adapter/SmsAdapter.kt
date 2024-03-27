package com.example.snitchsms.contacts.adapter

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.recyclerview.widget.RecyclerView
import com.example.snitchsms.R
import com.example.snitchsms.contacts.callbacks.SmsDelete
import com.example.snitchsms.contacts.model.SmsSaveModel
import com.example.snitchsms.dialog.SmsForwardDialog
import com.example.snitchsms.recieptdata.MessageListActivity.Companion.dateList
import com.example.snitchsms.recieptdata.MessageListActivity.Companion.datePosition
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale


class SmsAdapter(
    var item: ArrayList<SmsSaveModel>, val mContext: Context, val callBack: SmsDelete


) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        var flag: Boolean = false
        private const val VIEW_TYPE_RIGHT = 1
        private const val VIEW_TYPE_LEFT = 2


    }

    fun updateData(newData: ArrayList<SmsSaveModel>) {
        item.clear()
        item.addAll(newData)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return item.size
    }

    override fun getItemViewType(position: Int): Int {
        val model = item[position]
        return if (model.side == "Right") {
            VIEW_TYPE_RIGHT
        } else {
            VIEW_TYPE_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_TYPE_RIGHT -> {
                val viewRight = inflater.inflate(R.layout.mic_chat_item, parent, false)
                RightViewHolder(viewRight)
            }

            VIEW_TYPE_LEFT -> {
                val viewLeft = inflater.inflate(R.layout.mic_chat_item_left, parent, false)
                LeftViewHolder(viewLeft)
            }

            else -> throw IllegalArgumentException("Invalid view type")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val model = item[position]

        when (holder.itemViewType) {
            VIEW_TYPE_RIGHT -> {
                val rightViewHolder = holder as RightViewHolder

                rightViewHolder.bind(model)
            }

            VIEW_TYPE_LEFT -> {
                val leftViewHolder = holder as LeftViewHolder
                leftViewHolder.bind(model)
            }
        }

    }

    inner class RightViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtSmsBody: TextView = itemView.findViewById(R.id.txtSmsBody)
        private val txtDatetime: TextView = itemView.findViewById(R.id.txtdatetime)
        val txtDate: TextView = itemView.findViewById(R.id.txtdate)
        private val mConstraintRight: ConstraintLayout =
            itemView.findViewById(R.id.mConstraintRight)

        private var lastDisplayedDate: String? = ""
        private var isDateDisplayed = false

        /*   fun bind(model: SmsSaveModel) {
               txtSmsBody.text = model.message
               txtSmsBody.isSelected = true
               txtDatetime.text = model.date.toString()
               txtDatetime.isSelected = true

               deleteSms.setOnClickListener {
                   val position = adapterPosition
                   if (position != RecyclerView.NO_POSITION) {
                       callBack.itemDelete(position)
                   }
               }
           }*/
        @SuppressLint("NotifyDataSetChanged")
        fun bind(model: SmsSaveModel) {/* if (model.isBlocked) {
                 return
             }*/
            if (model.message.isEmpty()) {
                // If the message is empty, hide the item
                itemView.visibility = View.GONE
                itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            } else {
                // If not, show the item
                itemView.visibility = View.VISIBLE
                itemView.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                txtSmsBody.text = model.message
                txtSmsBody.isSelected = true
                txtDatetime.text = model.time
                txtDatetime.isSelected = true
                val currentDate = model.date.split(" ")[0]
                val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val sdf = SimpleDateFormat("yyyy-MM-dd hh:mm:ss")

                val date: Date = dateFormat.parse(currentDate)
                val nowdate1 = Date()
                val nowdate = sdf.format(Date())
                Log.d("Adapterer", "bind: dateforamt $date")
                Log.d("Adapterer", "bind: formattedDate $nowdate")


                val calendarModel = Calendar.getInstance().apply { time = date }
                val calendarCurrent = Calendar.getInstance().apply { time = nowdate1 }

                val formattedDate = when {
                    isSameDay(calendarModel, calendarCurrent) -> "Today"
                    isYesterday(calendarModel, calendarCurrent) -> "Yesterday"
                    else -> SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(date)
                }
                txtDate.text = formattedDate
                if (!dateList.contains(formattedDate)) {
                    dateList.add(formattedDate)
                    datePosition.add(adapterPosition)
                } else {
                    txtDate.text = ""
                }

                for (item in datePosition) {
                    Log.d("TAG", "bind: list postion ${item}")
                    Log.d("TAG", "bind: adapter postion ${adapterPosition}")
                    if (item == adapterPosition) {
                        Log.d("TAG", "bind:ccheck list postion ${item}")
                        txtDate.visibility = View.VISIBLE
                    } else {
//                        txtDate.visibility = View.GONE
                    }
                }

//                Log.d("Adapterer", "bind: currentDate $currentDate")
//                Log.d("Adapterer", "bind: lastDisplayedDate $lastDisplayedDate")
//                lastDisplayedDate = currentDate
                /*  if (!isDateDisplayed || currentDate != lastDisplayedDate) {
                      Log.d("Adapterer", "bind: Displaying date")
                      txtDate.visibility = View.VISIBLE
                      txtDate.text = model.date
                      lastDisplayedDate = currentDate
                      isDateDisplayed = true
                  } else {
                      Log.d("Adapterer", "bind: currentDate == lastDisplayedDate")
                      txtDate.visibility = View.GONE
                  }*//* if (!isDateDisplayed) {

                     Log.d("Adapterer", "bind: Displaying date for the first time")
                     txtDate.visibility = View.VISIBLE
                     txtDate.text = model.date
                     lastDisplayedDate = currentDate
                     isDateDisplayed = true
                 }
                 else if (currentDate != lastDisplayedDate) {
                     Log.d("Adapterer", "bind: currentDate != lastDisplayedDate ")
                     txtDate.visibility = View.VISIBLE
                     txtDate.text = model.date
                     lastDisplayedDate = currentDate
                     isDateDisplayed=false
                 } else {
                     Log.d("Adapterer", "bind: currentDate == lastDisplayedDate")
                     txtDate.visibility = View.GONE
                 }
 */
                mConstraintRight.setOnLongClickListener {
                    Log.d("TAG", "bind: mConstraintLeft click long")
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        showCustomPopupMenuRight(itemView)
                    }
                    true
                }
            }

        }

        fun isSameDay(cal1: Calendar, cal2: Calendar): Boolean {

            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(
                Calendar.MONTH
            ) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH)
        }

        fun isYesterday(cal1: Calendar, cal2: Calendar): Boolean {
            return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) && cal1.get(Calendar.MONTH) == cal2.get(
                Calendar.MONTH
            ) && cal1.get(Calendar.DAY_OF_MONTH) == cal2.get(Calendar.DAY_OF_MONTH) - 1
        }

        private fun showCustomPopupMenuRight(anchorView: View) {
            val inflater =
                anchorView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView = inflater.inflate(R.layout.popup_menu, null)

            val txtCopy = popupView.findViewById<TextView>(R.id.txtCopy)
            val txtDelete = popupView.findViewById<TextView>(R.id.txtDelete)
            val txtShare = popupView.findViewById<TextView>(R.id.txtShare)
            val txtStar = popupView.findViewById<TextView>(R.id.txtStarMessage)
            val txtForward = popupView.findViewById<TextView>(R.id.txtForward)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )


            val xOff = anchorView.width
            val yOff = -anchorView.height + 180

            // Show the PopupWindow
            popupWindow.showAsDropDown(anchorView, xOff, yOff)

            txtForward.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {

                    val forwardDialog = SmsForwardDialog(
                        itemView.context,
                        (itemView.context as ViewModelStoreOwner),
                        (itemView.context as LifecycleOwner).lifecycle,
                        item[position].message
                    )
                    forwardDialog.show()
                }
                popupWindow.dismiss()
            }
            txtCopy.setOnClickListener {

                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = item[position]
                    val textToCopy = item.message
                    val clipboardManager =
                        mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("text", textToCopy)
                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(mContext, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                popupWindow.dismiss()
            }

            txtDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    callBack.itemDelete(position)
                }
                popupWindow.dismiss()
            }
            txtStar.setOnClickListener {
                val imgStar = itemView.findViewById<ImageView>(R.id.imgStar)
                if (imgStar.visibility == View.VISIBLE) {
                    imgStar.visibility = View.GONE
                    txtStar.text = "Star Message"
                } else {
                    imgStar.visibility = View.VISIBLE
                    txtStar.text = "Unstar Message"
                }
                popupWindow.dismiss()
            }
            txtShare.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = item[position]
                    val textToShare = item.message
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textToShare)
                    }
                    if (sendIntent.resolveActivity(mContext.packageManager) != null) {
                        mContext.startActivity(sendIntent)
                    } else {
                        Toast.makeText(
                            mContext, "No apps available for sharing", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                popupWindow.dismiss()
            }
        }
    }

    inner class LeftViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val txtSmsBody: TextView = itemView.findViewById(R.id.txtSmsBodyLeft)
        private val txtDatetime: TextView = itemView.findViewById(R.id.txtdatetimeLeft)
        private val mConstraintLeft: ConstraintLayout = itemView.findViewById(R.id.mConstraintLeft)


        fun bind(model: SmsSaveModel) {/* if (model.isBlocked) {
                 return
             }*/
            if (model.message.isEmpty()) {
                itemView.visibility = View.GONE
                itemView.layoutParams = RecyclerView.LayoutParams(0, 0)
            } else {
                // If not, show the item
                itemView.visibility = View.VISIBLE
                itemView.layoutParams = RecyclerView.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
                )
                txtSmsBody.text = model.message
                txtSmsBody.isSelected = true
                txtDatetime.text = model.time
                txtDatetime.isSelected = true

                mConstraintLeft.setOnLongClickListener {
                    Log.d("TAG", "bind: mConstraintLeft click long")
                    val position = adapterPosition
                    if (position != RecyclerView.NO_POSITION) {
                        callBack.itemLongClick(position)

                        showCustomPopupMenu(itemView)
                    }
                    true
                }
            }

        }

        private fun showCustomPopupMenu(anchorView: View) {
            val inflater =
                anchorView.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView = inflater.inflate(R.layout.popup_menu, null)

            val txtCopy = popupView.findViewById<TextView>(R.id.txtCopy)
            val txtDelete = popupView.findViewById<TextView>(R.id.txtDelete)
            val txtShare = popupView.findViewById<TextView>(R.id.txtShare)
            val txtStar = popupView.findViewById<TextView>(R.id.txtStarMessage)
            val txtForward = popupView.findViewById<TextView>(R.id.txtForward)

            val popupWindow = PopupWindow(
                popupView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true
            )


            val xOff = anchorView.width
            val yOff = -anchorView.height + 180

            popupWindow.showAsDropDown(anchorView, xOff, yOff)

            txtForward.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {

                    val forwardDialog = SmsForwardDialog(
                        itemView.context,
                        (itemView.context as ViewModelStoreOwner),
                        (itemView.context as LifecycleOwner).lifecycle,
                        item[position].message
                    )
                    forwardDialog.show()
                }
                popupWindow.dismiss()
            }
            txtCopy.setOnClickListener {

                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = item[position]
                    val textToCopy = item.message
                    val clipboardManager =
                        mContext.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    val clipData = ClipData.newPlainText("text", textToCopy)
                    clipboardManager.setPrimaryClip(clipData)
                    Toast.makeText(mContext, "Text copied to clipboard", Toast.LENGTH_SHORT).show()
                }
                popupWindow.dismiss()
            }

            txtDelete.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    callBack.itemDelete(position)
                }
                popupWindow.dismiss()
            }
            txtStar.setOnClickListener {
                val imgStar = itemView.findViewById<ImageView>(R.id.imgStar)
                if (imgStar.visibility == View.VISIBLE) {
                    imgStar.visibility = View.GONE
                    txtStar.text = "Star Message"
                } else {
                    imgStar.visibility = View.VISIBLE
                    txtStar.text = "Unstar Message"
                }
                popupWindow.dismiss()
            }
            txtShare.setOnClickListener {
                val position = adapterPosition
                if (position != RecyclerView.NO_POSITION) {
                    val item = item[position]
                    val textToShare = item.message
                    val sendIntent = Intent().apply {
                        action = Intent.ACTION_SEND
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, textToShare)
                    }
                    if (sendIntent.resolveActivity(mContext.packageManager) != null) {
                        mContext.startActivity(sendIntent)
                    } else {
                        Toast.makeText(
                            mContext, "No apps available for sharing", Toast.LENGTH_SHORT
                        ).show()
                    }
                }
                popupWindow.dismiss()
            }
        }


    }

}
