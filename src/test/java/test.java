import com.c0ldcat.netease.music.NetEaseMusic;
import com.c0ldcat.netease.music.Playlist;
import com.c0ldcat.netease.music.Utils;

public class test {
    public static void main (String args[]) throws Exception{
        String username = System.getenv("NM_USER");
        String password = System.getenv("NM_PASS");
        String configFile = System.getenv("NM_CONFIG");
        String cacheDir = System.getenv("NM_CACHE");

        if ( username == null || password == null || configFile == null || cacheDir == null ) {
            Utils.log("launch failed");
            return;
        }

        NetEaseMusic netEaseMusic = new NetEaseMusic(configFile);
        //netEaseMusic.setCacheDir(cacheDir);
        //netEaseMusic.login(username, password);
        //netEaseMusic.updateUserPlaylist();
        Playlist playlist = netEaseMusic.getPlaylist(312201485);
        playlist.cacheAll();
        Utils.log(netEaseMusic.getCacheDir());
    }
}
