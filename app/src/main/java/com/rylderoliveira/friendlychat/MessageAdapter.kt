package com.rylderoliveira.friendlychat

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class MessageAdapter(
    private val items: MutableList<Message> = mutableListOf(),
) : RecyclerView.Adapter<MessageAdapter.MessageViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MessageViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_message, parent, false)
        return MessageViewHolder(view)
    }

    override fun onBindViewHolder(holder: MessageViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    fun addMessage(message: Message) {
        items.add(message)
        notifyItemInserted(itemCount)
    }

    fun removeMessage(message: Message) {
        items.remove(message)
        notifyItemRemoved(itemCount)
    }

    fun clear() {
        items.clear()
    }

    class MessageViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val textViewName: TextView = itemView.findViewById(R.id.text_view_item_name)
        private val textViewMessage: TextView = itemView.findViewById(R.id.text_view_item_message)

        fun bind(message: Message) {
            textViewName.text = message.userName
            textViewMessage.text = message.text
        }

    }
}