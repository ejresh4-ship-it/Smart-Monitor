package com.smartmonitor.app

import android.content.Context
import android.content.ComponentName
import android.content.Intent
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
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
    var playlists by remember { mutableStateOf(prefs.getString("playlists","{}") ?: "{}") }
    var folders by remember { mutableStateOf(prefs.getStringSet("folders",emptySet())?.toSet() ?: emptySet()) }
    var selectedPlaylist by remember { mutableStateOf<String?>(null) }
    var selectedFolder by remember { mutableStateOf<String?>(null) }
    var dialog by remember { mutableStateOf("") }
    var nameInput by remember { mutableStateOf("") }
    var expandedPlayer by remember { mutableStateOf(false) }
    var current by remember { mutableStateOf<MusicTrack?>(null) }
    var playing by remember { mutableStateOf(false) }
    var position by remember { mutableLongStateOf(0L) }
    var duration by remember { mutableLongStateOf(0L) }
    val scope= rememberCoroutineScope()
    val genreLabels=listOf("" to R.string.music_all,"Pop" to R.string.music_genre_pop,"Hip-Hop/Rap" to R.string.music_genre_rap,"Electronic" to R.string.music_genre_electronic,"Rock" to R.string.music_genre_rock,"R&B/Soul" to R.string.music_genre_rnb,"Jazz" to R.string.music_genre_jazz,"Classical" to R.string.music_genre_classical)
    fun playlistMap(): MutableMap<String, MutableSet<String>> {
        val result=linkedMapOf<String,MutableSet<String>>()
        try { val obj=JSONObject(playlists); obj.keys().forEach { key -> val a=obj.optJSONArray(key) ?: JSONArray(); result[key]=mutableSetOf<String>().apply { for(i in 0 until a.length()) add(a.optString(i)) } } } catch (_:Exception) {}
        return result
    }
    fun savePlaylists(map:Map<String,Set<String>>) {
        val obj=JSONObject(); map.forEach { (key,ids) -> obj.put(key,JSONArray(ids.toList())) }
        playlists=obj.toString(); prefs.edit().putString("playlists",playlists).apply()
    }
    fun toggleFavorite(track:MusicTrack) {
        val next=favorites.toMutableSet(); if(!next.add(track.id)) next.remove(track.id)
        favorites=next; prefs.edit().putStringSet("ids",next).apply()
    }
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
    DisposableEffect(player, tracks) {
        val listener=object:Player.Listener {
            override fun onIsPlayingChanged(value:Boolean){playing=value}
            override fun onMediaItemTransition(item:MediaItem?, reason:Int) { current = tracks.firstOrNull { it.id == item?.mediaId } }
            override fun onPlaybackStateChanged(state:Int) { duration=player?.duration?.coerceAtLeast(0L) ?: 0L }
        }
        player?.addListener(listener)
        onDispose { player?.removeListener(listener) }
    }
    LaunchedEffect(player) { while(true) { position=player?.currentPosition?.coerceAtLeast(0L) ?: 0L; duration=player?.duration?.coerceAtLeast(0L) ?: 0L; delay(500) } }
    val listFromPlaylist=selectedPlaylist?.let { playlistMap()[it].orEmpty() }
    val shown=when {
        selectedPlaylist!=null -> tracks.filter { listFromPlaylist?.contains(it.id)==true }
        favoritesOnly -> tracks.filter{favorites.contains(it.id)}
        else -> tracks
    }
    fun playFrom(list:List<MusicTrack>, track:MusicTrack) {
        current=track
        val queue=list.map { item ->
            val metadata=MediaMetadata.Builder().setTitle(item.title).setArtist(item.artist).apply { item.artwork?.let { setArtworkUri(Uri.parse(it)) } }.build()
            MediaItem.Builder().setMediaId(item.id).setUri("$AUDIO_API/tracks/${item.id}/stream?app_name=$AUDIO_APP").setMediaMetadata(metadata).build()
        }
        player?.setMediaItems(queue,list.indexOfFirst{it.id==track.id}.coerceAtLeast(0),0L)
        player?.prepare(); player?.play()
    }
    Scaffold(
        topBar={ TopAppBar(title={Column{Text("Music Studio",fontWeight=FontWeight.Bold);Text("Discover • Play • Organize",fontSize=11.sp)}},navigationIcon={IconButton(onClick=onBack){Icon(Icons.Default.ArrowBack,null)}},actions={IconButton(onClick={expandedPlayer=!expandedPlayer}){Icon(Icons.Default.Album,null)}}) },
        bottomBar={ current?.let{t-> Surface(shadowElevation=10.dp,color=Color(0xFF102A35)){Column(Modifier.fillMaxWidth().clickable{expandedPlayer=true}.padding(horizontal=12.dp, vertical=7.dp)){
            Row(verticalAlignment=Alignment.CenterVertically){
                if(t.artwork!=null) AsyncImage(t.artwork,null,Modifier.size(42.dp), contentScale=androidx.compose.ui.layout.ContentScale.Crop) else Icon(Icons.Default.MusicNote,null,tint=Color(0xFF65E6C2),modifier=Modifier.size(42.dp))
                Column(Modifier.weight(1f).padding(horizontal=10.dp)){Text(t.title,color=Color.White,maxLines=1,fontWeight=FontWeight.Bold);Text(t.artist,color=Color.LightGray,fontSize=11.sp,maxLines=1)}
                IconButton(onClick={player?.seekToPreviousMediaItem()}){Icon(Icons.Default.SkipPrevious,null,tint=Color.White)}
                IconButton(onClick={if(playing)player?.pause() else player?.play()}){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,tint=Color(0xFF65E6C2))}
                IconButton(onClick={player?.seekToNextMediaItem()}){Icon(Icons.Default.SkipNext,null,tint=Color.White)}
            }
            Slider(value=if(duration>0) position.toFloat().coerceIn(0f,duration.toFloat()) else 0f,onValueChange={position=it.toLong()},onValueChangeFinished={player?.seekTo(position)},valueRange=0f..(duration.coerceAtLeast(1L).toFloat()),modifier=Modifier.height(20.dp))
        }}} }
    ){padding->
        Column(Modifier.fillMaxSize().padding(padding).background(Color(0xFFF1F6F6)).padding(horizontal=14.dp)){
            if(expandedPlayer && current!=null) {
                val t=current!!
                Column(Modifier.fillMaxWidth().weight(1f),horizontalAlignment=Alignment.CenterHorizontally,verticalArrangement=Arrangement.Center){
                    if(t.artwork!=null) AsyncImage(t.artwork,null,Modifier.fillMaxWidth().aspectRatio(1f).padding(18.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop) else Box(Modifier.fillMaxWidth().aspectRatio(1f).padding(18.dp).background(Brush.linearGradient(listOf(Color(0xFF167C78),Color(0xFF172E52))),RoundedCornerShape(28.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,tint=Color.White,modifier=Modifier.size(90.dp))}
                    Text(t.title,fontSize=22.sp,fontWeight=FontWeight.Bold,color=Color(0xFF142F3A),maxLines=1)
                    Text(t.artist,color=Color.Gray)
                    Slider(value=if(duration>0)position.toFloat().coerceIn(0f,duration.toFloat()) else 0f,onValueChange={position=it.toLong()},onValueChangeFinished={player?.seekTo(position)},valueRange=0f..duration.coerceAtLeast(1L).toFloat())
                    Row(Modifier.fillMaxWidth().padding(horizontal=18.dp),horizontalArrangement=Arrangement.SpaceBetween){Text(formatMusicTime(position),fontSize=11.sp);Text(formatMusicTime(duration),fontSize=11.sp)}
                    Row(verticalAlignment=Alignment.CenterVertically,horizontalArrangement=Arrangement.Center){
                        IconButton(onClick={player?.seekToPreviousMediaItem()}){Icon(Icons.Default.SkipPrevious,null,modifier=Modifier.size(38.dp))}
                        IconButton(onClick={if(playing)player?.pause() else player?.play()}){Icon(if(playing)Icons.Default.Pause else Icons.Default.PlayArrow,null,modifier=Modifier.size(58.dp),tint=Color(0xFF168D78))}
                        IconButton(onClick={player?.seekToNextMediaItem()}){Icon(Icons.Default.SkipNext,null,modifier=Modifier.size(38.dp))}
                        IconButton(onClick={toggleFavorite(t)}){Icon(if(favorites.contains(t.id))Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=Color(0xFFE34C78))}
                    }
                    TextButton(onClick={expandedPlayer=false}){Text("Back to library")}
                }
            } else {
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(query,{query=it;favoritesOnly=false;selectedPlaylist=null},modifier=Modifier.fillMaxWidth(),singleLine=true,placeholder={Text(stringResource(R.string.music_search))},leadingIcon={Icon(Icons.Default.Search,null)},shape=RoundedCornerShape(16.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp),modifier=Modifier.padding(vertical=7.dp)){
                    FilterChip(selected=!latest,onClick={latest=false;favoritesOnly=false;selectedPlaylist=null},label={Text(stringResource(R.string.music_trending))})
                    FilterChip(selected=latest,onClick={latest=true;favoritesOnly=false;selectedPlaylist=null},label={Text(stringResource(R.string.music_latest))})
                    FilterChip(selected=favoritesOnly,onClick={favoritesOnly=!favoritesOnly;selectedPlaylist=null},label={Text("♥ Favorites")})
                }
                Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){
                    Button(onClick={dialog="playlist";nameInput=""}){Icon(Icons.Default.QueueMusic,null);Text("New playlist")}
                    OutlinedButton(onClick={dialog="folder";nameInput=""}){Icon(Icons.Default.Folder,null);Text("New folder")}
                    if(selectedPlaylist!=null) TextButton(onClick={selectedPlaylist=null}){Text("All tracks")}
                }
                if(folders.isNotEmpty()) Row(horizontalArrangement=Arrangement.spacedBy(6.dp)){folders.forEach{folder->FilterChip(selected=selectedFolder==folder,onClick={selectedFolder=if(selectedFolder==folder)null else folder;selectedPlaylist=null},label={Text(folder)},leadingIcon={Icon(Icons.Default.Folder,null)})}}
                if(selectedFolder!=null) playlistMap().keys.filter { it.startsWith("$selectedFolder/") }.forEach { pl ->
                    TextButton(onClick={selectedPlaylist=pl;favoritesOnly=false}){Icon(Icons.Default.QueueMusic,null);Text(pl.substringAfter("/"))}
                }
                if(selectedPlaylist==null && !favoritesOnly) {
                    val pls=playlistMap().keys.filter { selectedFolder==null || it.startsWith("$selectedFolder/") }
                    if(pls.isNotEmpty()) Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){pls.take(3).forEach{pl->AssistChip(onClick={selectedPlaylist=pl},label={Text(pl.substringAfterLast("/"),maxLines=1)})}}
                }
                Text("Genres",fontWeight=FontWeight.Bold,color=Color(0xFF173A42),modifier=Modifier.padding(top=5.dp,bottom=3.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){genreLabels.take(4).forEach{(g,l)->FilterChip(selected=genre==g,onClick={genre=g;latest=false;favoritesOnly=false},label={Text(stringResource(l),fontSize=10.sp)})}}
                Row(horizontalArrangement=Arrangement.spacedBy(4.dp)){genreLabels.drop(4).forEach{(g,l)->FilterChip(selected=genre==g,onClick={genre=g;latest=false;favoritesOnly=false},label={Text(stringResource(l),fontSize=10.sp)})}}
                if(loading) LinearProgressIndicator(Modifier.fillMaxWidth().padding(vertical=6.dp))
                if(failed){Text(stringResource(R.string.music_error),color=MaterialTheme.colorScheme.error);TextButton(onClick={scope.launch{reload()}}){Text(stringResource(R.string.music_retry))}}
                LazyColumn(Modifier.weight(1f),contentPadding=PaddingValues(vertical=8.dp),verticalArrangement=Arrangement.spacedBy(7.dp)){
                    items(shown,key={it.id}){track->
                        Row(Modifier.fillMaxWidth().background(Color.White,RoundedCornerShape(14.dp)).clickable{playFrom(shown,track)}.padding(9.dp),verticalAlignment=Alignment.CenterVertically){
                            if(track.artwork!=null) AsyncImage(track.artwork,null,Modifier.size(52.dp),contentScale=androidx.compose.ui.layout.ContentScale.Crop) else Box(Modifier.size(52.dp).background(Brush.linearGradient(listOf(Color(0xFF178D76),Color(0xFF173D59))),RoundedCornerShape(11.dp)),contentAlignment=Alignment.Center){Icon(Icons.Default.MusicNote,null,tint=Color.White)}
                            Column(Modifier.weight(1f).padding(horizontal=9.dp)){Text(track.title,maxLines=1,fontWeight=FontWeight.SemiBold,color=Color(0xFF18343B));Text(track.artist,maxLines=1,fontSize=12.sp,color=Color.Gray)}
                            IconButton(onClick={toggleFavorite(track)}){Icon(if(favorites.contains(track.id))Icons.Default.Favorite else Icons.Default.FavoriteBorder,null,tint=if(favorites.contains(track.id))Color(0xFFE44E75) else Color.Gray)}
                            IconButton(onClick={nameInput="";dialog="add_track:${track.id}"}){Icon(Icons.Default.PlaylistAdd,null,tint=Color(0xFF178D76))}
                            Icon(Icons.Default.PlayArrow,null,tint=Color(0xFF178D76))
                        }
                    }
                }
                Text("Catalog: Audius • Browse: Radio Javan • Spotify • SoundCloud • Jamendo",fontSize=10.sp,color=Color.Gray,modifier=Modifier.padding(bottom=5.dp))
                Row(horizontalArrangement=Arrangement.spacedBy(5.dp)){
                    TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.radiojavan.com")))}){Text("Radio Javan")}
                    TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://open.spotify.com")))}){Text("Spotify")}
                    TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://soundcloud.com")))}){Text("SoundCloud")}
                    TextButton(onClick={context.startActivity(Intent(Intent.ACTION_VIEW,Uri.parse("https://www.jamendo.com")))}){Text("Jamendo")}
                }
            }
        }
    }
    if(dialog.isNotEmpty()) {
        AlertDialog(onDismissRequest={dialog=""},title={Text(if(dialog=="folder")"Create folder" else if(dialog=="playlist")"Create playlist" else "Add to playlist")},
            text={
                if(dialog.startsWith("add_track:")) Column {
                    playlistMap().keys.forEach { pl -> TextButton(onClick={
                        val id=dialog.substringAfter(":"); val map=playlistMap(); map[pl]=map[pl] ?: mutableSetOf(); map[pl]?.add(id); savePlaylists(map); dialog=""
                    }){Text(pl)} }
                    if(playlistMap().isEmpty()) Text("Create a playlist first.")
                } else OutlinedTextField(nameInput,{nameInput=it},singleLine=true,label={Text("Name")})
            },
            confirmButton={if(!dialog.startsWith("add_track:")) TextButton(onClick={
                val n=nameInput.trim()
                if(n.isNotEmpty()) {
                    if(dialog=="folder") { folders=folders+n; prefs.edit().putStringSet("folders",folders).apply() }
                    else { val map=playlistMap(); map[if(selectedFolder!=null)"$selectedFolder/$n" else n]=mutableSetOf(); savePlaylists(map) }
                }
                dialog=""
            }){Text("Save")}},
            dismissButton={TextButton(onClick={dialog=""}){Text("Cancel")}})
    }
}

private fun formatMusicTime(ms:Long):String {
    val seconds=(ms.coerceAtLeast(0L)/1000L).toInt()
    return "%d:%02d".format(seconds/60,seconds%60)
}
