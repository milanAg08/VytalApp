package com.example.vytal


import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button

class CommunityFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        val view = inflater.inflate(R.layout.fragment_community, container, false)

        val btnJoin = view.findViewById<Button>(R.id.btnJoinCommunities)
        btnJoin.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.home_container, CommunityGroupsFragment())
                .addToBackStack(null)
                .commit()
        }

        return view
    }
}