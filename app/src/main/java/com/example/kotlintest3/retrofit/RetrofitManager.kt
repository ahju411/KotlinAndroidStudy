package com.example.kotlintest3.retrofit

import android.util.Log
import com.example.kotlintest3.model.Photo
import com.example.kotlintest3.utils.API
import com.example.kotlintest3.utils.Constants.TAG
import com.example.kotlintest3.utils.RESPONSE_STATUS
import com.google.gson.JsonElement
import retrofit2.Call
import retrofit2.Response
import java.text.SimpleDateFormat

class RetrofitManager {

    companion object{
        val instance = RetrofitManager()
    }

    //레트로핏 인터페이스 가져오기
    private val iRetrofit : IRetrofit? = RetrofitClient.getClient(API.BASE_URL)?.create(IRetrofit::class.java)

    //사진 검색 api 호출
    fun searchPhotos(searchTerm: String?, completion: (RESPONSE_STATUS, ArrayList<Photo>?) -> Unit){ //기존에 String으로 받은 건 안에 Json보려고 했던 거고 실제로 받아온 걸 ArrayList로 넣는다.

        val term = searchTerm ?: "" //searchTerm이 비어있으면 ""를 넣어라
        val call = iRetrofit?.searchPhotos(searchTerm = term) ?: return

        call.enqueue(object : retrofit2.Callback<JsonElement>{

            //응답 성공시
            override fun onResponse(call: Call<JsonElement>, response: Response<JsonElement>) {
                Log.d(TAG, "RetrofitManager - onResponse() called / response: ${response.body()}")

                when(response.code()){ //Client에서 들어온 코드가 200일 때만 실행하게 됨. 안정성 부여
                    200 -> {

                        response.body()?.let{
                            var parsedPhotoDataArray = ArrayList<Photo>() //받아올 parse를 arraylist로 만듦
                            val body = it.asJsonObject //전체 response 바디를 가져온다.
                            val results = body.getAsJsonArray("results") //그 부분에서 results인 부분만 추출한다.
                            
                            val total = body.get("total").asInt //그 부분에서 total만 추출한다.

                            Log.d(TAG, "onResponse: /total $total")

                            //데이터가 없으면 no_content로 보낸다.
                            if(total == 0){
                                completion(RESPONSE_STATUS.NO_CONTENT, null)
                            }else{ //데이터가 있다면
                                results.forEach { resultItem ->
                                    val resultItemObject = resultItem.asJsonObject

                                    val user = resultItemObject.get("user").asJsonObject

                                    val username : String = user.get("username").asString

                                    val likesCount = resultItemObject.get("likes").asInt

                                    val thumbnailLink = resultItemObject.get("urls").asJsonObject.get("thumb").asString

                                    val createdAt = resultItemObject.get("created_at").asString

                                    val parser = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss") //날짜포매팅 설정하기
                                    val formatter = SimpleDateFormat("yyyy년\nMM월 dd일")

                                    val outputDateString= formatter.format(parser.parse(createdAt))

                                    Log.d(TAG, "onResponse: / outputDateString $outputDateString")

                                    val photoItem = Photo(author = username, likesCount = likesCount, thumbnail = thumbnailLink, createdAt = outputDateString)

                                    parsedPhotoDataArray.add(photoItem)
                                }

                                completion(RESPONSE_STATUS.OKAY,parsedPhotoDataArray)
                            }


                        }
                    }
                }
            }

            //응답 실패시시
           override fun onFailure(call: Call<JsonElement>, t: Throwable) {
                Log.d(TAG, "RetrofitManager - onFailure() called / t: $t" )

                completion(RESPONSE_STATUS.FAIL, null)
            }

        })

    }


}