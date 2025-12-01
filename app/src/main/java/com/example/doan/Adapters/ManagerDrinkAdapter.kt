package com.example.doan.Adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.doan.Models.Drink
import com.example.doan.R
import java.text.NumberFormat
import java.util.Locale

class ManagerDrinkAdapter(
    private val context: Context,
    private var drinkList: MutableList<Drink>,
    private val listener: OnDrinkActionListener
) : RecyclerView.Adapter<ManagerDrinkAdapter.ViewHolder>() {

    private var drinkListFull: List<Drink> = ArrayList(drinkList)

    interface OnDrinkActionListener {
        fun onEditClick(drink: Drink)
        fun onDeleteClick(drink: Drink)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_manager_drink, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val drink = drinkList[position]

        holder.tvName.text = drink.name
        holder.tvCategory.text = drink.categoryName ?: "N/A"

        // Format price - use basePrice from Drink model
        val currencyFormat = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        holder.tvPrice.text = currencyFormat.format(drink.basePrice)

        // Load image
        if (!drink.imageUrl.isNullOrEmpty()) {
            Glide.with(context)
                .load(drink.imageUrl)
                .placeholder(R.drawable.ic_image_placeholder)
                .error(R.drawable.ic_broken_image)
                .into(holder.imgDrink)
        } else {
            holder.imgDrink.setImageResource(R.drawable.ic_image_placeholder)
        }

        // Edit button
        holder.btnEdit.setOnClickListener {
            listener.onEditClick(drink)
        }

        // Delete button
        holder.btnDelete.setOnClickListener {
            listener.onDeleteClick(drink)
        }
    }

    override fun getItemCount(): Int = drinkList.size

    fun updateList(newList: List<Drink>) {
        this.drinkList = newList.toMutableList()
        this.drinkListFull = ArrayList(newList)
        notifyDataSetChanged()
    }

    fun filter(query: String) {
        drinkList.clear()
        if (query.isEmpty()) {
            drinkList.addAll(drinkListFull)
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            drinkList.addAll(drinkListFull.filter { drink ->
                drink.name?.lowercase()?.contains(lowerCaseQuery) == true ||
                drink.categoryName?.lowercase()?.contains(lowerCaseQuery) == true
            })
        }
        notifyDataSetChanged()
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgDrink: ImageView = itemView.findViewById(R.id.img_drink)
        val tvName: TextView = itemView.findViewById(R.id.tv_drink_name)
        val tvCategory: TextView = itemView.findViewById(R.id.tv_drink_category)
        val tvPrice: TextView = itemView.findViewById(R.id.tv_drink_price)
        val btnEdit: ImageButton = itemView.findViewById(R.id.btn_edit)
        val btnDelete: ImageButton = itemView.findViewById(R.id.btn_delete)
    }
}
