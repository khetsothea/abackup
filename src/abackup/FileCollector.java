/*
 * Cognity
 * Copyright (c) 2007 FinAnalytica, Inc.
 * All Rights Reserved.
 *
 * 2007-11-26 - Alex - created
 */
package abackup;

import java.util.*;
import java.io.File;
import java.io.FilenameFilter;

public class FileCollector {
    private Map<String, Set<String>> included;

    public FileCollector() {
        included = new LinkedHashMap<String, Set<String>>();
    }
    
    public void collect(String pattern, boolean isAtomic) {
        for (Map.Entry<String, String> entry : expandFiles(pattern, isAtomic).entrySet()) {
            if (entry.getValue() != null) {
                Set<String> groups = included.get(entry.getKey());
                if (groups == null) {
                    groups = new LinkedHashSet<String>();
                    included.put(entry.getKey(), groups);
                }
                groups.add(entry.getValue());
            } else {
                if (!included.containsKey(entry.getKey())) {
                    included.put(entry.getKey(), null);
                }
            }
        }
    }

    public Map<String, Set<String>> collectedNames() {
        return included;
    }

    public void removeAll(List<String> namePatterns) {
        List<WildcharFilenameFilter> excludeFilters = new ArrayList<WildcharFilenameFilter>(namePatterns.size());
        for (String excludePattern : namePatterns) {
            if (new File(excludePattern).getParent() == null) {
                excludeFilters.add(new WildcharFilenameFilter(excludePattern));
            }
        }
        Iterator<String> it = included.keySet().iterator();
        while (it.hasNext()) {
            String s = it.next();
            for (WildcharFilenameFilter excludeFilter : excludeFilters) {
                if (excludeFilter.accept(s)) {
                    it.remove();
                    break;
                }
            }
        }
    }

    public void retainAll(List<String> namePatterns) {
        List<WildcharFilenameFilter> includeFilters = new ArrayList<WildcharFilenameFilter>(namePatterns.size());
        for (String includePattern : namePatterns) {
            if (new File(includePattern).getParent() == null) {
                includeFilters.add(new WildcharFilenameFilter(includePattern));
            }
        }
        Iterator<String> it = included.keySet().iterator();
        while (it.hasNext()) {
            String s = it.next();
            boolean isAccepted = true;
            for (WildcharFilenameFilter includeFilter : includeFilters) {
                if (!includeFilter.accept(s)) {
                    isAccepted = false;
                    break;
                }
            }
            if (!isAccepted) {
                it.remove();
            }
        }
    }

    private Map<String, String> expandFiles(String pattern, boolean isAtomic) {
        Map<String, String> expanded = new LinkedHashMap<String, String>();
        List<String> patterns = expandPatterns(pattern);
        for (String patt : patterns) {
            Set<String> files = new LinkedHashSet<String>();
            expandFile(new File(patt), files);
            for (String file : files) {
                expanded.put(file, isAtomic ? patt : null);
            }
        }
        return expanded;
    }

    public static List<String> expandPatterns(String pattern) {
        File file = new File(pattern);
        File parent = file.getParentFile();
        if (parent == null) {
            return Collections.singletonList(pattern);
        } else {
            List<String> parents = expandPatterns(parent.getAbsolutePath());
            String name = file.getName();
            List<String> result = new LinkedList<String>();
            if (name.indexOf('*') >= 0 || name.indexOf('?') >= 0) {
                for (String expandedParentPath : parents) {
                    FilenameFilter filter = new WildcharFilenameFilter(name);
                    File expandedParent = new File(expandedParentPath);
                    if (expandedParent.isDirectory()) {
                        for (String s : expandedParent.list(filter)) {
                            result.add(new File(expandedParent, s).getAbsolutePath());
                        }
                    }
                }
            } else {
                for (String expandedParentPath : parents) {
                    result.add(new File(expandedParentPath, name).getAbsolutePath());
                }
            }
            return result;
        }
    }

    private void expandFile(File file, Set<String> expanded) {
        if (file.isDirectory()) {
            for (String s : file.list()) {
                expandFile(new File(file, s), expanded);
            }
        } else if (file.exists()) {
            expanded.add(file.getAbsolutePath());
            if (expanded.size() % 100 == 0) {
                System.out.print(".");
            }
        }
    }

    private static class WildcharFilenameFilter implements FilenameFilter {
        String regex;

        WildcharFilenameFilter(String pattern) {
            regex = pattern.replaceAll("\\\\", "\\\\\\\\").replaceAll("\\.", "\\\\.").replaceAll("\\*", ".*").replaceAll("\\?", ".?");
        }

        public boolean accept(String name) {
            return name.matches(regex);
        }

        public boolean accept(File dir, String name) {
            return accept(name);
        }
    }
}
