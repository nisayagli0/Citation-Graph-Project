package com.nisa.prolabproje3;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class CitationGraph {
    // Map to find all articles by ID.
    private Map<String, Article> articleMap = new HashMap<>();
    
    public List<Article> readJsonFile(String filePath) {
        List<Article> articleList = new ArrayList<>();
        Article tempArticle = null;
        boolean readingAuthors = false;
        boolean readingReferences = false;

        System.out.println("Reading file: " + filePath);
        // Opens the file and reads it piece by piece.
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.equals("{")){
                    tempArticle = new Article();
                }
                else if (line.equals("},") || line.equals("}")){
                    if (tempArticle != null) articleList.add(tempArticle);
                    tempArticle = null;
                } 
                else if (tempArticle != null){
                    if (line.startsWith("\"id\":")){
                        tempArticle.setId(clean(line, "\"id\":"));
                    }
                    else if (line.startsWith("\"title\":")){
                        tempArticle.setTitle(clean(line, "\"title\":"));
                    } 
                    else if (line.startsWith("\"year\":")){
                        try {
                            tempArticle.setYear(Integer.parseInt(line.replace("\"year\":", "").replace(",", "").trim())); 
                        } 
                        catch (Exception e) {
                        }
                    } 
                    else if (line.contains("\"authors\": [")){
                        readingAuthors = true;
                    } 
                    else if (readingAuthors){
                        if (line.contains("]")) readingAuthors = false;
                        else { String a = clean(line, ""); if(!a.isEmpty()) tempArticle.addAuthor(a); }
                    } 
                    else if (line.contains("\"referenced_works\": [") || line.contains("\"referenced works\": [")) {
                        if (!line.contains("]")) readingReferences = true;
                    } 
                    else if (readingReferences){
                        if (line.contains("]")) readingReferences = false;
                        else { String r = clean(line, ""); if(!r.isEmpty()) tempArticle.addReference(r); }
                    }
                }
            }
        } catch (IOException e){
            e.printStackTrace();
        }

        // Convert List to Map.
        for (Article article : articleList){
            articleMap.put(article.getId(), article);
        }

        // Calculate which article cites which.
        for (Article sourceArticle : articleList){
            for (String targetId : sourceArticle.getReferences()){
                Article targetArticle = articleMap.get(targetId);
                if (targetArticle != null){
                    targetArticle.incrementCitationCount();
                    // Record reverse relationship.
                    targetArticle.addCitingPaper(sourceArticle.getId()); 
                }
            }
        }

        // Sort by Article ID.
        Collections.sort(articleList, (m1, m2) -> m1.getId().compareTo(m2.getId())); // Sorts by ID order.
        for (int i = 0; i < articleList.size(); i++){
            Article m = articleList.get(i);
            if (i > 0) m.previous = articleList.get(i - 1);
            if (i < articleList.size() - 1) m.next = articleList.get(i + 1);
        }
        
        return articleList;
    }
    
    private String clean(String line, String tag){
        return line.replace(tag, "").replace("\"", "").replace(",", "").trim();
    }

    public Article findArticle(String id){
        return articleMap.get(id);
    }
    
    public Article findMostCited(List<Article> list){
        if (list == null || list.isEmpty()) return null;
        Article top = list.get(0);
        for (Article m : list) 
            if (m.getCitationCount() > top.getCitationCount()) 
                top = m;
        return top;
    }

    public Article findMostReferencing(List<Article> list){
        if (list == null || list.isEmpty()) return null;
        Article top = list.get(0);
        for (Article m : list) 
            if (m.getReferences().size() > top.getReferences().size()) 
                top = m;
        return top;
    }

    // H-Index, H-Core, H-Median Calculation
    public List<Article> getHCoreList(Article article){
        List<Article> citingArticles = new ArrayList<>();
        // Find all articles that cite this one.
        for (String id : article.getCitingPaperIds()){ 
            Article m = articleMap.get(id);
            if (m != null) citingArticles.add(m);
        }
        // Sort from largest to smallest based on citation count
        citingArticles.sort((m1, m2) -> Integer.compare(m2.getCitationCount(), m1.getCitationCount()));

        int h = 0;
        for (int i = 0; i < citingArticles.size(); i++){
            if (citingArticles.get(i).getCitationCount() >= (i + 1)) h = i + 1;
            else 
                break;
        }
        
        if (h > 0 && h <= citingArticles.size()) 
            return citingArticles.subList(0, h);
        return new ArrayList<>();
    }

    public int calculateHIndex(Article article){
        return getHCoreList(article).size();
    }

    public double calculateHMedian(Article article){
        List<Article> core = getHCoreList(article);
        if (core.isEmpty()) return 0;
        List<Integer> citations = new ArrayList<>(); // Put only citation counts into a list.
        for (Article m : core) citations.add(m.getCitationCount());
        Collections.sort(citations); // Sort citation counts
        
        int n = citations.size();
        if (n % 2 == 1) 
            return citations.get(n / 2);
        else 
            return (citations.get((n / 2) - 1) + citations.get(n / 2)) / 2.0;
    }

    // K-Core Calculation
    public List<Article> performKCoreAnalysis(int k){
        Set<String> activeIds = new HashSet<>(articleMap.keySet());
        
        // Calculate references + citations
        Map<String, Integer> degrees = new HashMap<>();
        Map<String, List<String>> neighbors = new HashMap<>(); 

        for (String id : activeIds){
            Article m = articleMap.get(id);
            Set<String> uniqueNeighbors = new HashSet<>(); // To avoid double counting
            
            for(String ref : m.getReferences()) 
                if(activeIds.contains(ref)) uniqueNeighbors.add(ref);
            for(String incoming : m.getCitingPaperIds()) 
                if(activeIds.contains(incoming)) uniqueNeighbors.add(incoming);
            
            neighbors.put(id, new ArrayList<>(uniqueNeighbors));
            degrees.put(id, uniqueNeighbors.size());
        }

        boolean changeOccurred = true;
        while (changeOccurred){
            changeOccurred = false;
            List<String> toRemove = new ArrayList<>();

            for (String id : activeIds){
                if (degrees.get(id) < k){
                    toRemove.add(id);
                }
            }

            if (!toRemove.isEmpty()){
                changeOccurred = true;
                for (String removedId : toRemove){
                    activeIds.remove(removedId);
                    
                    // If a neighbor is removed, decrease degree.
                    for (String neighbor : neighbors.get(removedId)){
                        if (activeIds.contains(neighbor)){
                            int oldDegree = degrees.get(neighbor);
                            degrees.put(neighbor, oldDegree - 1);
                        }
                    }
                }
            }
        }
        // List the remaining ones
        List<Article> result = new ArrayList<>();
        for (String id : activeIds){
            result.add(articleMap.get(id));
        }
        return result;
    }

    // Betweenness
    public Map<String, Double> calculateBetweenness(){
        Map<String, Double> betweenness = new HashMap<>();
        for (String id : articleMap.keySet()) betweenness.put(id, 0.0); 

        // Adjacency list for shortest path calculations
        Map<String, List<String>> adjacencyList = new HashMap<>();
        for (Article m : articleMap.values()){
            Set<String> links = new HashSet<>();
            links.addAll(m.getReferences());
            links.addAll(m.getCitingPaperIds());
            
            List<String> validLinks = new ArrayList<>();
            for (String b : links) 
                if (articleMap.containsKey(b)) 
                    validLinks.add(b);
            adjacencyList.put(m.getId(), validLinks);
        }

        // Run BFS for each node 's' (Brandes Algorithm)
        for (String s : articleMap.keySet()){
            Stack<String> stack = new Stack<>();
            Queue<String> queue = new LinkedList<>();
            queue.add(s);

            Map<String, List<String>> predecessors = new HashMap<>(); 
            for(String k : articleMap.keySet()) predecessors.put(k, new ArrayList<>());

            Map<String, Integer> sigma = new HashMap<>(); // Number of shortest paths
            for(String k : articleMap.keySet()) sigma.put(k, 0);
            sigma.put(s, 1);

            Map<String, Integer> distances = new HashMap<>(); 
            for(String k : articleMap.keySet()) distances.put(k, -1);
            distances.put(s, 0);

            while (!queue.isEmpty()){
                String v = queue.poll();
                stack.push(v);

                for (String w : adjacencyList.get(v)){
                    if (distances.get(w) < 0) {
                        queue.add(w);
                        distances.put(w, distances.get(v) + 1);
                    }
                    if (distances.get(w) == distances.get(v) + 1){
                        sigma.put(w, sigma.get(w) + sigma.get(v));
                        predecessors.get(w).add(v);
                    }
                }
            }

            Map<String, Double> delta = new HashMap<>();
            for(String k : articleMap.keySet()) delta.put(k, 0.0);

            while (!stack.isEmpty()){
                String w = stack.pop();
                for (String v : predecessors.get(w)){
                    double fraction = (double) sigma.get(v) / sigma.get(w);
                    delta.put(v, delta.get(v) + fraction * (1.0 + delta.get(w)));
                }
                if (!w.equals(s)){
                    betweenness.put(w, betweenness.get(w) + delta.get(w));
                }
            }
        }
        
        // Divide score by 2 since it's an undirected graph logic here.
        for (String key : betweenness.keySet()){
            betweenness.put(key, betweenness.get(key) / 2.0);
        }

        return betweenness;
    }
}