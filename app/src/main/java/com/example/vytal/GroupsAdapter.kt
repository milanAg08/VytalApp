package com.example.vytal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class GroupsAdapter(
    private val groups: MutableList<Group>,
    private val currentUserId: String
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.groupName)
        val desc: TextView = itemView.findViewById(R.id.groupDesc)
        val btnJoin: MaterialButton = itemView.findViewById(R.id.btnJoin)
        val tvMemberCount: TextView = itemView.findViewById(R.id.tvMemberCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        holder.name.text = group.name
        holder.desc.text = group.description
        holder.tvMemberCount.text = "${group.memberCount} members"
        
        // Check if user is already a member
        db.collection("community_groups")
            .document(group.id)
            .get()
            .addOnSuccessListener { doc ->
                val members = doc.get("members") as? List<*> ?: emptyList<Any>()
                val isMember = members.any { it.toString() == currentUserId }
                
                if (isMember) {
                    holder.btnJoin.text = "LEAVE"
                    holder.btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5252.toInt())) // Red
                } else {
                    holder.btnJoin.text = "JOIN"
                    holder.btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt())) // Green
                }
            }
        
        // Join/Leave button click
        holder.btnJoin.setOnClickListener {
            joinOrLeaveGroup(group, position, holder)
        }

        // Card click - navigate to group detail
        holder.itemView.setOnClickListener {
            try {
                val fragment = GroupDetailFragment()
                fragment.arguments = bundleOf("groupId" to group.id)

                val activity = holder.itemView.context as? AppCompatActivity
                if (activity != null) {
                    val containerId = when (activity::class.java.simpleName) {
                        "MainActivity" -> R.id.fragment_container
                        "HomeActivity" -> R.id.home_container
                        else -> {
                            if (activity.findViewById<View>(R.id.fragment_container) != null) {
                                R.id.fragment_container
                            } else {
                                R.id.home_container
                            }
                        }
                    }
                    
                    activity.supportFragmentManager.beginTransaction()
                        .replace(containerId, fragment)
                        .addToBackStack(null)
                        .commit()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    
    private fun joinOrLeaveGroup(group: Group, position: Int, holder: GroupViewHolder) {
        val groupRef = db.collection("community_groups").document(group.id)
        
        groupRef.get().addOnSuccessListener { doc ->
            val members = doc.get("members") as? List<*> ?: emptyList<Any>()
            val memberIds = members.mapNotNull { it.toString() }.toMutableList()
            val isMember = memberIds.contains(currentUserId)
            
            if (isMember) {
                // Leave group
                memberIds.remove(currentUserId)
                groupRef.update("members", memberIds)
                    .addOnSuccessListener {
                        groups[position] = group.copy(memberCount = group.memberCount - 1)
                        holder.btnJoin.text = "JOIN"
                        holder.btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF4CAF50.toInt()))
                        holder.tvMemberCount.text = "${groups[position].memberCount} members"
                        Toast.makeText(holder.itemView.context, "Left ${group.name}", Toast.LENGTH_SHORT).show()
                    }
            } else {
                // Join group
                if (!memberIds.contains(currentUserId)) {
                    memberIds.add(currentUserId)
                }
                groupRef.update("members", memberIds)
                    .addOnSuccessListener {
                        groups[position] = group.copy(memberCount = group.memberCount + 1)
                        holder.btnJoin.text = "LEAVE"
                        holder.btnJoin.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFFFF5252.toInt()))
                        holder.tvMemberCount.text = "${groups[position].memberCount} members"
                        Toast.makeText(holder.itemView.context, "Joined ${group.name}!", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
    
    override fun getItemCount(): Int = groups.size
}
