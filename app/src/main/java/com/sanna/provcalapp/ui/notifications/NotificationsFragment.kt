package com.sanna.provcalapp.ui.notifications

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.sanna.provcalapp.databinding.FragmentNotificationsBinding
import com.sanna.provcalapp.feature.schedule.data.DefaultScheduleRepository
import com.sanna.provcalapp.feature.schedule.domain.ScheduleRepository

class NotificationsFragment : Fragment() {

    private var _binding: FragmentNotificationsBinding? = null
    private val binding get() = _binding!!

    private val repo: ScheduleRepository = DefaultScheduleRepository()
    private lateinit var vm: NotificationsViewModel

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentNotificationsBinding.inflate(inflater, container, false)

        vm = ViewModelProvider(this, object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return NotificationsViewModel(repo) as T
            }
        })[NotificationsViewModel::class.java]

        observeUi()
        vm.load()
        return binding.root
    }

    private fun observeUi() {
        vm.next.observe(viewLifecycleOwner) { d ->
            d ?: return@observe
            binding.tvPillDayNum.text = d.dayNum
            binding.tvPillDow.text    = d.dayName.take(3).uppercase()
            binding.tvNextStart.text  = d.start ?: "—"
            binding.tvNextEnd.text    = d.end   ?: "—"
            binding.tvNextTotal.text  = d.total ?: "—"
        }

        vm.week.observe(viewLifecycleOwner) { list ->
            val slots = listOf(
                binding.itemDay1, binding.itemDay2, binding.itemDay3,
                binding.itemDay4, binding.itemDay5, binding.itemDay6
            )
            val upcoming = list.drop(1).take(slots.size)
            slots.zip(upcoming).forEach { (itBinding, d) ->
                itBinding.tvDateNum.text = d.dayNum
                itBinding.tvDayName.text = d.dayName.take(3).uppercase()
                itBinding.tvStart.text   = d.start ?: "—"
                itBinding.tvEnd.text     = d.end   ?: "—"
                itBinding.tvTotal.text   = d.total ?: "—"
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}