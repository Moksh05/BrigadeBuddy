package com.example.brigadebuddy
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.brigadebuddy.databinding.OccasionCardBinding
import com.example.brigadebuddy.db.EventEntity

class RecyclerViewAdapter(

    private val onItemClick: (EventEntity) -> Unit,
    private val onItemLongClick: (EventEntity) -> Unit

) : ListAdapter<EventEntity, RecyclerViewAdapter.EventViewHolder>(DiffCallback) {

    object DiffCallback : DiffUtil.ItemCallback<EventEntity>() {
        override fun areItemsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean =
            oldItem.id == newItem.id

        override fun areContentsTheSame(oldItem: EventEntity, newItem: EventEntity): Boolean =
            oldItem == newItem
    }

    inner class EventViewHolder(
        private val binding: OccasionCardBinding
    ) : RecyclerView.ViewHolder(binding.root) {



        fun bind(event: EventEntity) {

            val monthNames = arrayOf(
                "Jan", "Feb", "Mar", "Apr", "May", "Jun",
                "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"
            )
            val rankAndName = "${event.rank} ${event.firstname}".trim()

            binding.rvTitle.text = event.title
            binding.rvName.text = rankAndName
            binding.rvDate.text = "${event.day}"
            binding.rvMonth.text = monthNames[event.month-1]


            binding.root.setOnClickListener { onItemClick(event) }
            binding.root.setOnLongClickListener {
                onItemLongClick(event)
                true
            }
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EventViewHolder {
        val binding = OccasionCardBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return EventViewHolder(binding)
    }

    override fun onBindViewHolder(holder: EventViewHolder, position: Int) {
        holder.bind(getItem(position))
    }
}