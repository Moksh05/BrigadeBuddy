package com.example.brigadebuddy

import android.app.DatePickerDialog
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.brigadebuddy.databinding.ItemChildBinding
import com.example.brigadebuddy.model.ChildForm
import java.util.Calendar
import androidx.core.widget.doAfterTextChanged

class ChildAdapter(
    private val children: MutableList<ChildForm>
) : RecyclerView.Adapter<ChildAdapter.ChildVH>() {

    inner class ChildVH(val binding: ItemChildBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChildVH {
        val binding = ItemChildBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ChildVH(binding)
    }

    override fun getItemCount(): Int = children.size

    override fun onBindViewHolder(holder: ChildVH, position: Int) {
        val child = children[position]
        val b = holder.binding

        // name
        b.etChildName.setText(child.name)
        if (child.birthYear != 0) {
            b.etChildDob.setText("${child.birthDay}-${child.birthMonth}-${child.birthYear}")
        }
        b.etChildName.doAfterTextChanged {
            child.name = it.toString()
        }

        // gender spinner
        val genders = listOf("MALE", "FEMALE")
        val adapter = ArrayAdapter(
            b.root.context,
            android.R.layout.simple_spinner_item,
            genders
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        b.spinnerChildGender.adapter = adapter

        b.spinnerChildGender.setSelection(
            if (child.gender == "FEMALE") 1 else 0
        )

        b.spinnerChildGender.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {

                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    child.gender = parent?.getItemAtPosition(position).toString()
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
            }


        // DOB picker
        b.etChildDob.setOnClickListener {
            val cal = Calendar.getInstance()
            DatePickerDialog(
                b.root.context,
                { _, y, m, d ->
                    child.birthDay = d
                    child.birthMonth = m + 1
                    child.birthYear = y
                    b.etChildDob.setText("$d-${m+1}-$y")
                },
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            ).show()
        }

        // delete child
        b.btnRemoveChild.setOnClickListener {
            val pos = holder.adapterPosition
            if (pos != RecyclerView.NO_POSITION) {
                children.removeAt(pos)
                notifyItemRemoved(pos)
            }
        }
    }
}
