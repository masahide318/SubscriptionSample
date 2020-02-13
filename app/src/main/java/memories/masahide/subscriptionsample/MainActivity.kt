package memories.masahide.subscriptionsample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider

class MainActivity : AppCompatActivity() {

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_main)
    val viewModel = ViewModelProvider(
      this,
      MainViewModelFactory(MyBillingClient())
    ).get(
      MainActivityViewModel::class.java
    )
    viewModel.skuDetails.observe(this, Observer {
      //商品情報取得完了と同時に購入処理を呼び出す
      viewModel.subscribe(it[0], this)
    })
    viewModel.purchase.observe(this, Observer {
      //定期購読の購入完了を伝える
    })
    viewModel.startConnection()
  }

}
