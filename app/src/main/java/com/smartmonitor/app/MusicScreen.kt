package com.smartmonitor.app

import android.content.Context
import android.content.ComponentName
import android.net.Uri
import androidx.media3.common.MediaMetadata
import coil.compose.AsyncImage
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL

private const val AUDIO_API = "https://discoveryprovider.audius.co/v1"
private const val AUDIO_APP = "SmartMonitorAndroid"
private data class MusicTrack(val id:String,val title:String,val artist:String,val duration:Int,val artwork:String?)

private suspend fun fetchMusic(query:String, genre:String, latest:Boolean):List<MusicTrack> = withContext(Dispatchers.IO) {
    val path = when {
        query.isNotBlank() -> "/tracks/search?query=${URLEncoder.encode(query,"UTF-8")}&limit=30"
        latest -> "/tracks/latest?limit=30"
        else -> "/tracks/trending?limit=30" + if (genre.isNotBlank()) "&genre=${URLEncoder.encode(genre,"UTF-8")}" else ""
    }
    val url=URL("$AUDIO_API$path&app_name=$AUDIO_APP")
    val c=url.openConnection() as HttpURLConnection
    c.connectTimeout=12000; c.readTimeout=15000
    try {
        val json=JSONObject(c.inputStream.bufferedReader().use{it.readText()})
        val data=json.optJSONArray("data") ?: return@withContext emptyList()
        buildList {
            for(i in 0 until data.length()){
                val t=data.optJSONObject(i) ?: continue
                val id=t.optString("id").takeIf{it.isNotBlank()} ?: t.optString("track_id")
                if(id.isNotBlank()) add(MusicTrack(id,t.optString("title","Untitled"),t.optJSONObject("user")?.optString("name","Unknown artist") ?: "Unknown artist",t.optInt("duration"),t.optJSONObject("artwork")?.optString("480x480")?.takeIf { it.isNotBlank() }))
            }
        }
    } finally { c.disconnect() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(onBack:()->Unit) {
    val context= LocalContext.current
    val prefs= remember { context.getSharedPreferences("smart_music",Context.MODE_PRIVATE) }
    var player by remember { mutableStateOf<MediaController?>(null) }
    val controllerFuture = remember {
        val token = SessionToken(context, ComponentName(context, MusicPlaybackService::class.java))
        MediaController.Builder(context, token).buildAsync()
    }
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var query by remember { mutableStateOf("") }
    var genre by remember { mutableStateOf("") }
    var latest by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var favoritesOnly by remember { mutableStateOf(false) }
    var favorites by remember { mutableStateOf(prefs.getStringSet("ids",emptySet())?.toSet() ?: emptySet()) }
    var current by remember { mutableStateOf<MusicTrack?>(null) }
    var playing by remember { mutableStateOf(false) }
    val scope= rememberCoroutineScope()
    val genreLabels=listOf("" to R.string.music_all,"Pop" to R.string.music_genre_pop,"Hip-Hop/Rap" to R.string.music_genre_rap,"Electronic" to R.string.music_genre_electronic,"Rock" to R.string.music_genre_rock,"R&B/Soul" to R.string.music_genre_rnb,"Jazz" to R.string.music_genre_jazz,"Classical" to R.string.music_genre_classical)
    suspend fun reload() {
        loading=true; failed=false
        try { tracks=fetchMusic(query.trim(),genre,latest) } catch (_:Exception) { failed=true }
        finally { loading=false }
    }
    LaunchedEffect(query,genre,latest) { delay(if(query.isBlank()) 0 else 500); reload() }
    DisposableEffect(controllerFuture) {
        controllerFuture.addListener({ try { player = controllerFuture.get() } catch (_:Exception) { } }, java.util.concurrent.Executor { it.run() })
        onDispose { MediaController.releaseFuture(controllerFuture) }
    }
    DisposableEffect(player) {
        val listener=object:Player.Listener { override fun onIsPlayingChanged(value:Boolean){playing=value} }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    val shown=if(favoritesOnly) tracks.filter{favorites.contains(it.id)} else tracks
    Scaffold(
        topBar={ TopAppBar(title={Column{Text(stringResource(R.string.music_title),fontWeight=FontWeight.Bold);Text(stringResource(R.string.music_subtitle),fontSize=11.sp)}},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)}},actions={IconButton(onClick={favoritesOnly=!favoritesOnly}){Icon(if(favoritesOnly) Icons.Default.Favorite else Icons.Default.FavoriteBorder,null)}}) },
        bottomBar={ current?.let{t-> Surface(shadowElevation=8.dp,color=Color(0xFF17333B)){Row(Modifier.fillMaxWidth().padding(12.dp),verticalAlignment=Alignment.CenterVertically){Icon(Icons.Default.MusicNote,null,tint=Color(0xFF64E6BE),modifier=Modifier.size(32.dp));Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(t.title,color=Color.White,maxLines=1,fontWeight=FontWeight.Bold);Text(t.artist,color=Color.LightGray,fontSize=12.sp,maxLines=1)};IconButton(onClick={if(player?.isPlaying == true) player?.pause() else player?.play()}){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=Color.White)}}}} }
    ){padding->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF4F8F7)).padding(horizontal=14.dp)){
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(query,{query=it;favoritesOnly=false},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(stringResource(R.string.music_search))},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(16.dp))
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                FilterChip(selected=!latest,onClick={latest=false;favoritesOnly=false},label={Text(stringResource(R.string.music_trending))})
                FilterChip(selected=latest,onClick={latest=true;favoritesOnly=false},label={Text(stringResource(R.string.music_latest))})
                FilterChip(selected=favoritesOnly,onClick={favoritesOnly=!favoritesOnly},label={Text(stringResource(R.string.music_favorites))})
            }
            Text(stringResource(R.string.music_categories),fontWeight=FontWeight.Bold,color=Color(0xFF173A42),modifier=Modifier.padding(top=8.dp,bottom=4.dp))
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){genreLabels.take(4).forEach{(g,l)->FilterChip(selected=genre==g,onClick={genre=g;latest=false;favoritesOnly=false},label={Text(stringResource(l),fontSize=10.sp)})}}
            Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){genreLabels.drop(4).forEach{(g,l)->FilterChip(selected=genre==g,onClick={genre=g;latest=false;favoritesOnly=false},label={Text(stringResource(l),fontSize=10.sp)})}}
            if(loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=6.dp))
            if(failed){Text(stringResource(R.string.music_error),color=MaterialTheme.colorScheme.error,modifier=Modifier.padding(10.dp));TextButton(onClick={scope.launch{reload()}}){Icon(Icons.Default.Refresh,null);Text(stringResource(R.string.music_retry))}}
            else if(!loading && shown.isEmpty()) Text(stringResource(R.string.music_empty),color=Color.Gray,modifier=Modifier.padding(12.dp))
            LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                items(shown,key={it.id}){track->
                    Row(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(14.dp)).clickable{
                        current=track
                        val queue = shown.map { item ->
                            val metadata = MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist)
                                .apply { item.artwork?.let { setArtworkUri(Uri.parse(it)) } }.build()
                            MediaItem.Builder().setMediaId(item.id)
                                .setUri("$AUDIO_API/tracks/${item.id}/stream?app_name=$AUDIO_APP")
                                .setMediaMetadata(metadata).build()
                        }
                        val startAt = shown.indexOfFirst { it.id == track.id }.coerceAtLeast(0)
                        player?.setMediaItems(queue, startAt, 0L)
                        player?.prepare()
                        player?.play()
                    }.padding(10.dp),verticalAlignment=Alignment.CenterVertically){
                        if(track.artwork!=null) AsyncImage(model=track.artwork,contentDescription=null,modifier=Modifier.size(46.dp)) else Box(Modifier.size(46.dp).background(Brush.linearGradient(listOf(Color(0xFF178D76),Color(0xFF173D59))),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,tint=Color.White)}
                        Column(Modifier.weight(1f).padding(horizontal=9.dp)){Text(track.title,maxLines=1,fontWeight=FontWeight.SemiBold,color=Color(0xFF18343B));Text(track.artist,maxLines=1,fontSize=12.sp,color=Color.Gray)}
                        IconButton(onClick={val next=favorites.toMutableSet();if(!next.add(track.id))next.remove(track.id);favorites=next;prefs.edit().putStringSet("ids",next).apply()}){Icon(if(favorites.contains(track.id))Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(favorites.contains(track.id))Color(0xFFE44E75) else Color.Gray)}
                        Icon(Icons.Default.PlayArrow,null,tint=Color(0xFF178D76))
                    }
                }
            }
            Text(stringResource(R.string.music_source_note),fontSize=10.sp,color=Color.Gray,modifier=Modifier.padding(bottom=7.dp))
        }
    }
}
