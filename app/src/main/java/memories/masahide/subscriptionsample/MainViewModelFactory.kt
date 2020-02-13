package memories.masahide.subscriptionsample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

class MainViewModelFactory(private val myBillingClient: MyBillingClient) :
    ViewModelProvider.Factory {

    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        return MainActivityViewModel(myBillingClient) as T
    }
}