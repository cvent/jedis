package redis.clients.util;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CventSharded<R, S extends ShardInfo<R>> {

    private List<S> nodes;
    private final Hashing algo;
    private final Map<ShardInfo<R>, R> resources = new LinkedHashMap<ShardInfo<R>, R>();

    /**
     * The default pattern used for extracting a key tag. The pattern must have a group (between parenthesis), which
     * delimits the tag to be hashed. A null pattern avoids applying the regular expression for each lookup, improving
     * performance a little bit is key tags aren't being used.
     */
    private Pattern tagPattern = null;
    // the tag is anything between {}
    public static final Pattern DEFAULT_KEY_TAG_PATTERN = Pattern
            .compile("\\{(.+?)\\}");

    public CventSharded(List<S> shards) {
        this(shards, Hashing.MURMUR_HASH);
    }

    public CventSharded(List<S> shards, Hashing algo) {
        this.algo = algo;
        initialize(shards);
    }

    public CventSharded(List<S> shards, Pattern tagPattern) {
        this(shards, Hashing.MURMUR_HASH, tagPattern); // MD5 is really not good
        // as we works with
        // 64-bits not 128
    }

    public CventSharded(List<S> shards, Hashing algo, Pattern tagPattern) {
        this.algo = algo;
        this.tagPattern = tagPattern;
        initialize(shards);
    }

    private void initialize(List<S> shards) {
        nodes = new ArrayList<S>();

        for (int i = 0; i != shards.size(); ++i) {
            final S shardInfo = shards.get(i);
            nodes.add(shardInfo);
            resources.put(shardInfo, shardInfo.createResource());
        }
    }

    public R getShard(byte[] key) {
        return resources.get(getShardInfo(key));
    }

    public R getShard(String key) {
        return resources.get(getShardInfo(key));
    }

    public S getShardInfo(byte[] key) {
        return nodes.get((int) (algo.hash(key) % nodes.size()));
    }

    public S getShardInfo(String key) {
        return getShardInfo(SafeEncoder.encode(getKeyTag(key)));
    }

    public Collection<S> getAllShardInfo() {
        return nodes;
    }

    public Collection<R> getAllShards() {
        return Collections.unmodifiableCollection(resources.values());
    }

    /**
     * A key tag is a special pattern inside a key that, if preset, is the only part of the key hashed in order to
     * select the server for this key.
     *
     * @see http://code.google.com/p/redis/wiki/FAQ#I 'm_using_some_form_of_key_hashing_for_partitioning,_but_wh
     * @param key
     * @return The tag if it exists, or the original key
     */
    public String getKeyTag(String key) {
        if (tagPattern != null) {
            Matcher m = tagPattern.matcher(key);
            if (m.find()) {
                return m.group(1);
            }
        }
        return key;
    }

}