package com.example.vytal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class GroupsAdapter(
    private val groups: ArrayList<Group>,
    private val onClick: (Group) -> Unit
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    inner class GroupViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val groupName: TextView = view.findViewById(R.id.groupName)
        val groupDesc: TextView = view.findViewById(R.id.groupDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val g = groups[position]
        holder.groupName.text = g.name
        holder.groupDesc.text = g.description

        holder.itemView.setOnClickListener {
            onClick(g)
        }
    }

    override fun getItemCount(): Int = groups.size
}
