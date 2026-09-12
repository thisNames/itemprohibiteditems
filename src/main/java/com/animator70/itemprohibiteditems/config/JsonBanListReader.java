package com.animator70.itemprohibiteditems.config;

import java.io.IOException;
import java.io.Reader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import net.minecraftforge.fml.loading.FMLPaths;

import com.animator70.itemprohibiteditems.ItemProhibitedItems;

/**
 * 读取 config 目录下以 MOD_ID 命名的文件夹里的 JSON 黑名单。
 *
 * 目录结构约定为：
 * 
 * <pre>
 * config/
 * └── itemprohibiteditems/ &lt;-- 本模组目录（{@link ItemProhibitedItems#MOD_ID}）
 * ├── minecraft/
 * │ ├── items.json &lt;-- 纯字符串数组 ["diamond_sword", "bow", ...]
 * │ └── wearables.json
 * └── some_mod/
 * └── items.json
 *
 * 子目录名即为「分类 / 命名空间」，JSON 文件内容是一个纯字符串数组，且只需写「短 id」
 * （不带命名空间，例如 {@code "diamond_sword"}）。本类会自动拼成完整 id：
 * {@code <子目录名>:<短 id>}。例如目录 {@code minecraft} + 短 id {@code diamond_sword}
 * {@code "minecraft:diamond_sword"}。
 *
 * 采用 live-read：每次调用都直接读盘，改完文件后无需重启即可拿到最新结果；
 * 每次调用都会返回一个全新的、不可变的 {@link Set} 快照，可安全赋给外部缓存引用。
 *
 * 全程只读：目录或 JSON 文件不存在时直接跳过、返回空集合，绝不主动创建任何目录或文件；
 * 读取失败同样静默跳过，不做任何日志输出。
 */
public final class JsonBanListReader {
    // Json 解析器
    private static final Gson GSON = new Gson();

    // JSON 文件名
    private static final String ITEM_FILENAME = "items.json";
    private static final String WEARABLE_FILENAME = "wearables.json";

    // 纯字符串数组对应的反序列化类型：List<String>
    private static final Type STRING_LIST_TYPE = new TypeToken<List<String>>() {
    }.getType();

    private JsonBanListReader() {
    }

    /**
     * 返回所有子目录中 items.json 的内容。
     * 集合项格式：{@code "<子目录名>:<短 id>"}。
     */
    public static Set<String> getItems() {
        return loadAll(ITEM_FILENAME);
    }

    /**
     * 返回所有子目录中 wearables.json 的内容。
     * 集合项格式：{@code "<子目录名>:<短 id>"}。
     */
    public static Set<String> getWearables() {
        return loadAll(WEARABLE_FILENAME);
    }

    /**
     * 遍历 MOD_ID 目录下的每一个子目录，读取其中指定文件名的 JSON 并汇总到结果集合。
     */
    private static Set<String> loadAll(String fileName) {
        // config 目录下的本模组目录
        Path modConfigDir = FMLPaths.CONFIGDIR.get().resolve(ItemProhibitedItems.MOD_ID);

        // 目录不存在就不读，也不主动创建任何目录/文件
        if (!Files.isDirectory(modConfigDir)) {
            return Collections.emptySet();
        }

        // 结果集合
        Set<String> result = new HashSet<>();

        // 读取子目录
        try (DirectoryStream<Path> subDirs = Files.newDirectoryStream(modConfigDir, Files::isDirectory)) {
            for (Path subDir : subDirs) {
                // 子目录下的 JSON 文件
                Path jsonFile = subDir.resolve(fileName);

                // JSON 文件不存在就跳过
                if (!Files.isRegularFile(jsonFile)) {
                    continue;
                }

                // 读取模组子目录下的 JSON 文件
                readJsonArray(jsonFile, subDir.getFileName().toString(), result);
            }
        } catch (IOException ignored) {
            // 静默跳过，不输出任何日志
        }

        // 返回全新的不可变快照，外部缓存可安全持有
        return Set.copyOf(result);
    }

    /**
     * 读取单个 JSON 文件（纯字符串数组），把每一项按 {@code "<子目录名>:<短 id>"} 拼入结果集合
     * 子目录名作为命名空间，JSON 项作为短 id
     */
    private static void readJsonArray(Path file, String dirName, Set<String> result) {
        try (Reader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            // 读取 JSON 文件内容
            List<String> items = GSON.fromJson(reader, STRING_LIST_TYPE);

            // JSON 文件内容为空或读取失败时跳过
            if (items == null) {
                return;
            }

            // 遍历 JSON 文件内容
            for (String item : items) {
                if (item == null) {
                    continue;
                }

                // 去掉短 id 首尾可能误输入的空格
                String shortId = item.trim();

                // 短 id 为空时跳过
                if (!shortId.isEmpty()) {
                    // 拼成完整 id：<子目录名>:<短 id>
                    result.add(dirName + ":" + shortId);
                }
            }
        } catch (IOException | RuntimeException ignored) {
            // JSON 结构不正确或读取失败时静默跳过，不输出任何日志
        }
    }
}
