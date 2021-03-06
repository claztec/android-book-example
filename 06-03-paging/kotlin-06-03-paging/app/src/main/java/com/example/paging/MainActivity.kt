package com.example.paging

import android.os.Bundle
import android.view.*
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.paging.LivePagedListBuilder
import androidx.paging.PageKeyedDataSource
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class MainActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var pokeAPI: PokeAPI

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        recyclerView = findViewById(R.id.recyclerView)
        val adapter = MainRecyclerViewAdapter()
        recyclerView.adapter = adapter
        val layoutManager = LinearLayoutManager(this)
        recyclerView.layoutManager = layoutManager

        val retrofit = Retrofit.Builder()
                .baseUrl("https://pokeapi.co/api/v2/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()
        pokeAPI = retrofit.create(PokeAPI::class.java)

        createLiveData().observe(this, Observer { results ->
            adapter.submitList(results)
        })
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.menu_main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        val id = item.itemId


        return if (id == R.id.action_settings) {
            true
        } else super.onOptionsItemSelected(item)

    }

    private fun createLiveData(): LiveData<PagedList<Result>> {
        val config = PagedList.Config.Builder()
                .setInitialLoadSizeHint(20)
                .setPageSize(20)
                .setPrefetchDistance(10)
                .build()
        return LivePagedListBuilder(object : androidx.paging.DataSource.Factory<String, Result>() {
            override fun create(): androidx.paging.DataSource<String, Result> {
                return DataSource()
            }
        }, config).build()
    }

    private inner class DataSource : PageKeyedDataSource<String, Result>() {

        override fun loadInitial(params: LoadInitialParams<String>, callback: LoadInitialCallback<String, Result>) {
            try {
                val body = pokeAPI.listPokemons().execute().body()
                body?.let { body ->
                    callback.onResult(body.results, body.previous, body.next)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        override fun loadBefore(params: LoadParams<String>, callback: LoadCallback<String, Result>) {
            val queryPart = params.key.split("?")[1]
            val queries = queryPart.split("&")
            val map = mutableMapOf<String, String>()
            for (query in queries) {
                val parts = query.split("=")
                map[parts[0]] = parts[1]
            }
            try {
                val body = pokeAPI.listPokemons(map["offset"]!!, map["limit"]!!).execute().body()
                body?.let { body ->
                    callback.onResult(body.results, body.previous)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }

        override fun loadAfter(params: LoadParams<String>, callback: LoadCallback<String, Result>) {
            val queryPart = params.key.split("?")[1]
            val queries = queryPart.split("&")
            val map = mutableMapOf<String, String>()
            for (query in queries) {
                val parts = query.split("=")
                map[parts[0]] = parts[1]
            }
            try {
                val body = pokeAPI.listPokemons(map["offset"]!!, map["limit"]!!).execute().body()
                body?.let { body ->
                    callback.onResult(body.results, body.next)
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    private class MainRecyclerViewAdapter : PagedListAdapter<Result, MainRecyclerViewViewHolder>(object : DiffUtil.ItemCallback<Result>() {
        override fun areItemsTheSame(oldItem: Result, newItem: Result): Boolean {
            return oldItem.name == newItem.name
        }

        override fun areContentsTheSame(oldItem: Result, newItem: Result): Boolean {
            return oldItem.name == newItem.name && oldItem.url == newItem.url
        }
    }) {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): MainRecyclerViewViewHolder {
            val itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_recyclerview, parent, false)
            return MainRecyclerViewViewHolder(itemView)
        }

        override fun onBindViewHolder(holder: MainRecyclerViewViewHolder, position: Int) {
            val item = getItem(position)
            holder.setTitle(item?.name)
        }
    }

    private class MainRecyclerViewViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val title: TextView = itemView.findViewById(R.id.title)

        fun setTitle(title: String?) {
            this.title.text = title
        }
    }
}
