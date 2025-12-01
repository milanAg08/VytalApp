package com.example.vytal



import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.os.bundleOf
import androidx.recyclerview.widget.RecyclerView

class GroupsAdapter(private val groups: List<Group>) :
    RecyclerView.Adapter<GroupsAdapter.GroupViewHolder>() {

    class GroupViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val name: TextView = itemView.findViewById(R.id.groupName)
        val desc: TextView = itemView.findViewById(R.id.groupDesc)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_group, parent, false)
        return GroupViewHolder(view)
    }

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {

        val group = groups[position]
        holder.name.text = groups[position].name
        holder.desc.text = groups[position].description


        holder.itemView.setOnClickListener {
            try {
                val fragment = GroupDetailFragment()
                fragment.arguments = bundleOf("groupId" to group.id)

                val activity = holder.itemView.context as? AppCompatActivity
                if (activity != null) {
                    // Determine which container to use based on activity type
                    val containerId = when (activity::class.java.simpleName) {
                        "MainActivity" -> R.id.fragment_container
                        "HomeActivity" -> R.id.home_container
                        else -> {
                            // Try to find which container exists
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
    override fun getItemCount(): Int = groups.size
}
