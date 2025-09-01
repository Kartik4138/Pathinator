package com.example.pathinator.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.example.pathinator.databinding.ItemPathsBinding

data class Session(val name: String, val date: String)
class PathAdapter(
    private val sessions: List<Session>,
    private val onClick: (Session) -> Unit
): RecyclerView.Adapter<PathAdapter.PathViewHolder>() {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): PathViewHolder {
        val binding = ItemPathsBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PathViewHolder(binding)
    }

    override fun onBindViewHolder(
        holder: PathViewHolder,
        position: Int
    ) {
        val session = sessions[position]
        holder.binding.tvSessionName.text = session.name
        holder.binding.tvSessionDate.text = session.date
        holder.binding.root.setOnClickListener { onClick(session) }
    }

    override fun getItemCount(): Int = sessions.size

    inner class PathViewHolder(val binding: ItemPathsBinding) :
        RecyclerView.ViewHolder(binding.root)


}