import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternUtils;
import org.springframework.core.type.classreading.CachingMetadataReaderFactory;
import org.springframework.core.type.classreading.MetadataReader;
import org.springframework.core.type.classreading.MetadataReaderFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 扫描器类
 *
 * 提供一个扫描器用于扫描指定路径下的类名
 *
 * 目前默认扫描spring项目下的所有类，不支持外部指定jar包
 *
 * @see Resource 数据资源，由此可得到各个类的详细信息
 * @see ResourcePatternResolver 获取资源的策略接口
 * @see MetadataReaderFactory 读取元数据的工厂接口
 * @see MetadataReader 读取resource的元数据
 *
 * @author cloudgu
 * @since 2020/12/14
 */
public class Scaner {

    private final ResourcePatternResolver resourcePatternResolver;
    private final MetadataReaderFactory metadataReaderFactory;

    public Scaner() {
        this.resourcePatternResolver = ResourcePatternUtils.getResourcePatternResolver(null);
        this.metadataReaderFactory = new CachingMetadataReaderFactory();
    }

    /**
     * 扫描并且打印结果
     */
    public void print() {
        List<String> allClassName = allClassName();

        allClassName = filterInvalidName(allClassName);

        Map<String, List<String>> suffixWithClassNamesMap = suffixWithClassNamesMap(allClassName);

        sortAndPrintResult(suffixWithClassNamesMap);

    }

    Map<String, List<String>> suffixWithClassNamesMap(List<String> allClassName) {
        Map<String, List<String>> result = new HashMap<>();
        allClassName.forEach(className -> {
            if (result.containsKey(suffix(className))) {
                // 已经存在相同后缀
               result.get(suffix(className)).add(className);
            } else {
                // 新后缀
                List<String> classNames = new ArrayList<>();
                classNames.add(className);
                result.put(suffix(className), classNames);
            }
        });
        return result;
    }

    /**
     * 过滤无效类名
     *
     * 过滤内部类
     * 过滤无大写的文件
     */
    private List<String> filterInvalidName(List<String> allClassName) {
        allClassName = allClassName.stream()
                .filter(className -> !className.contains("$"))
                .filter(this::existUpperCase)
                .collect(Collectors.toList());
        return allClassName;
    }

    private boolean existUpperCase(String className) {
        for (int i = 0; i < className.length(); i++) {
            if (className.charAt(i) <= 'Z' && className.charAt(i) >= 'A') {
                return true;
            }
        }
        return false;
    }

    /**
     * 排序并打印结果
     * @param suffixWithClassNamesMap 后缀及其对应的类名列表
     */
    private void sortAndPrintResult(Map<String, List<String>> suffixWithClassNamesMap) {
        suffixWithClassNamesMap
                .entrySet()
                .stream()
                .sorted((o1, o2) -> o2.getValue().size() - o1.getValue().size())
                .forEach(this::printResult);
    }

    private void printResult(Map.Entry<String, List<String>> suffixWithClassNames) {
        System.out.println(suffixWithClassNames.getValue().get(0));
    }

    /**
     * 获取类名后缀
     *
     * 根据最后一个大写字母来判断后缀
     * 例如，ResourceResolver的后缀为Resolver
     * @param className 类名
     * @return 后缀
     */
    private String suffix(String className) {
        int lastUpperCase = -1;
        for (int i = className.length() - 1; i >= 0; i--) {
            boolean isUpperCase = className.charAt(i) >= 'A' && className.charAt(i) <= 'Z';
            if (isUpperCase) {
                lastUpperCase = i;
                break;
            }
        }
        if (lastUpperCase == -1) {
            System.out.println("类名不存在大写字母：" + className);
            return className;
        } else {
            return className.substring(lastUpperCase);
        }
    }

    private List<String> allClassName() {
        List<String> result = new ArrayList<>();
        allResource().forEach(resource -> result.add(className(resource)));
        return result;
    }

    private String className(Resource resource) {
        try {
            MetadataReader metadataReader = metadataReaderFactory.getMetadataReader(resource);
            return metadataReader.getClassMetadata().getClassName();
        } catch (Exception e) {
            e.printStackTrace();
            return "";
        }
    }

    /**
     * 获取所有的resource
     * @return 指定路径下所有resource的汇总
     */
    private List<Resource> allResource() {
        List<Resource> result = new ArrayList<>();
        List<String> paths = springPackagePaths();
        paths.forEach(path -> {
            try {
                Resource[] resources = resourcePatternResolver.getResources(path);
                result.addAll(Arrays.stream(resources).collect(Collectors.toList()));
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        return result;
    }

    /**
     * 获取所有spring相关的包路径
     */
    private List<String> springPackagePaths() {
        String core = "classpath*:org/springframework/**/*.class";
        List<String> result = new ArrayList<>();
        result.add(core);
        return result;
    }


}
