package com.example.vytal

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.Chip
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.widget.Toast

class GroupsAdapter(
    private val groups: List<Group>,
    private val onGroupClick: (Group) -> Unit = {}
) : RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val currentUserId = auth.currentUser?.uid ?: ""

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.groupName)
        val desc: TextView = itemView.findViewById(R.id.groupDesc)
        val memberCount: Chip = itemView.findViewById(R.id.chipMemberCount)
        val btnJoinLeave: MaterialButton = itemView.findViewById(R.id.btnJoinLeave)
        val btnViewGroup: MaterialButton = itemView.findViewById(R.id.btnViewGroup)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_community_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        val group = groups[position]
        
        holder.name.text = group.name
        holder.desc.text = group.description
        holder.memberCount.text = "${group.memberCount} members"
        
        // Update join/leave button
        if (group.isJoined) {
            holder.btnJoinLeave.text = "Leave"
            holder.btnJoinLeave.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
            holder.btnJoinLeave.backgroundTintList = 
                holder.itemView.context.getColorStateList(android.R.color.holo_red_light)
        } else {
            holder.btnJoinLeave.text = "Join"
            holder.btnJoinLeave.setIconResource(android.R.drawable.ic_input_add)
            holder.btnJoinLeave.backgroundTintList = 
                holder.itemView.context.getColorStateList(R.color.buttonGreenDark)
        }
        
        // Join/Leave button click
        holder.btnJoinLeave.setOnClickListener {
            if (currentUserId.isEmpty()) {
                Toast.makeText(holder.itemView.context, "Please login to join groups", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (group.isJoined) {
                leaveGroup(group, holder)
            } else {
                joinGroup(group, holder)
            }
        }
        
        // View group button click
        holder.btnViewGroup.setOnClickListener {
            navigateToGroupDetail(group, holder)
        }
        
        // Card click
        holder.itemView.setOnClickListener {
            navigateToGroupDetail(group, holder)
        }
    }

    private fun joinGroup(group: Group, holder: GroupViewHolder) {
        if (currentUserId.isEmpty()) return
        
        // Add user to group members
        db.collection("community_groups")
            .document(group.id)
            .collection("members")
            .document(currentUserId)
            .set(hashMapOf(
                "joinedAt" to System.currentTimeMillis(),
                "userId" to currentUserId
            ))
            .addOnSuccessListener {
                // Add group to user's joined groups
                db.collection("users")
                    .document(currentUserId)
                    .collection("joined_groups")
                    .document(group.id)
                    .set(hashMapOf(
                        "groupId" to group.id,
                        "joinedAt" to System.currentTimeMillis()
                    ))
                    .addOnSuccessListener {
                        Toast.makeText(holder.itemView.context, "Joined ${group.name}!", Toast.LENGTH_SHORT).show()
                        // Update UI
                        holder.btnJoinLeave.text = "Leave"
                        holder.btnJoinLeave.setIconResource(android.R.drawable.ic_menu_close_clear_cancel)
                        holder.btnJoinLeave.backgroundTintList = 
                            holder.itemView.context.getColorStateList(android.R.color.holo_red_light)
                        
                        // Update member count
                        val newCount = group.memberCount + 1
                        holder.memberCount.text = "$newCount members"
                    }
            }
            .addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Error joining group", Toast.LENGTH_SHORT).show()
            }
    }

    private fun leaveGroup(group: Group, holder: GroupViewHolder) {
        if (currentUserId.isEmpty()) return
        
        // Remove user from group members
        db.collection("community_groups")
            .document(group.id)
            .collection("members")
            .document(currentUserId)
            .delete()
            .addOnSuccessListener {
                // Remove group from user's joined groups
                db.collection("users")
                    .document(currentUserId)
                    .collection("joined_groups")
                    .document(group.id)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(holder.itemView.context, "Left ${group.name}", Toast.LENGTH_SHORT).show()
                        // Update UI
                        holder.btnJoinLeave.text = "Join"
                        holder.btnJoinLeave.setIconResource(android.R.drawable.ic_input_add)
                        holder.btnJoinLeave.backgroundTintList = 
                            holder.itemView.context.getColorStateList(R.color.buttonGreenDark)
                        
                        // Update member count
                        val newCount = maxOf(0, group.memberCount - 1)
                        holder.memberCount.text = "$newCount members"
                    }
            }
            .addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Error leaving group", Toast.LENGTH_SHORT).show()
            }
    }

    private fun navigateToGroupDetail(group: Group, holder: GroupViewHolder) {
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
            Toast.makeText(holder.itemView.context, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount(): Int = groups.size
}
