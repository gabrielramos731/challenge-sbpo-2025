package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChallengeSolver {

    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int nOrders;
    protected int nAisles;
    protected int waveSizeLB;
    protected int waveSizeUB;

    protected ArrayList<Integer> orderItems;                 // orderItems[x] is total number of items in order x
    protected ArrayList<Integer> aisleItems;                 // aisleItems[x] is total number of items in aisle x

    protected ArrayList<Integer> itemsInOrders;              // itemsInOrders[x] is number of items x in all orders
    protected ArrayList<Integer> itemsInAisles;              // itemsInAisles[x] is number of items x in all aisles

    Random rng;

    //Guarda a soma total de itens em uma determinada corredor/pedido bem como o index da corredor/pedido no vetor original
    protected Map.Entry<Integer,Integer>[] sumSTotalUnitsAisle;
    protected Map.Entry<Integer,Integer>[] sumSTotalUnitsOrder;

    int mostComumItemInOrders;
    int mostComumItemInAisles;

    ArrayList<Boolean> bestOrderSelectionFound;
    ArrayList<Boolean> bestAisleSelectionFound;

    private static Map<String, List<Integer>> memo;
    private ArrayList<Integer> sortedBySizeOrders;
    public ChallengeSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        initialize();
    }

    int sum(int[] vetor, int n){
        int sum = 0;
        for(int i = 0; i < n; i++){
            sum += vetor[i];
        }
        return sum;
    }

    // orderna corredores e pedidos baseados na quantidade de itens
    private void heapfy(List<Integer> arr, int i, int n, ArrayList<Double> penaltyFactor){
        int min = i;
        int l = 2 * i +1;
        int r = 2 * i + 2;
        if(l < n && penaltyFactor.get(arr.get(l)) < penaltyFactor.get(arr.get(min))) min = l;
        if(r < n && penaltyFactor.get(arr.get(r)) < penaltyFactor.get(arr.get(min))) min = r;

       if(min != i){
           Integer temp = arr.get(i);
           arr.set(i, arr.get(min));
           arr.set(min, temp);
           heapfy(arr, min, n, penaltyFactor);
       }
    }

    private void heapSort(List<Integer> arr, ArrayList<Double> penaltyFactor){
        int n = arr.size();
        for(int i = n/2-1; i >= 0; i--){
            heapfy(arr, i, n, penaltyFactor);
        }
        for(int i = n-1; i >= 0; i--){
            Integer temp = arr.get(0);
            arr.set(0, arr.get(i));
            arr.set(i, temp);
            heapfy(arr, 0, i, penaltyFactor);
        }
    }

    // calcula o total dos itens idexados pelo id em um corredor ou pedido
    void calcAmountOfItens(Map<Integer, Integer> thing, ArrayList<Integer> amount){
        for(Map.Entry<Integer, Integer> entry : thing.entrySet()){
            Integer oldValue = amount.get(entry.getKey());
            amount.set(entry.getKey(), oldValue+ entry.getValue());
        }
    }


    ArrayList<Integer> generateItensInHand2(ArrayList<Boolean> selectedOrders, List<Integer> selectedAisles){
        ArrayList<Integer> inHandItens = new ArrayList<>();

        for(int i = 0; i < nItems; i++){
            inHandItens.add(0);
        }

        for(int i : selectedAisles){
            calcAmountOfItens(aisles.get(i), inHandItens);
        }

        for(int i=0;i<nOrders; i++){
            if(selectedOrders.get(i)){
                Map<Integer, Integer> order = orders.get(i);
                for(Map.Entry<Integer, Integer> entry : order.entrySet()){
                    int oldValue = inHandItens.get(entry.getKey());
                    inHandItens.set(entry.getKey(), oldValue - entry.getValue());
                }
            }
        }

        return inHandItens;
    }

    // calcula a quantidade de itens indexados pelo id nos corredores selecionados
    ArrayList<Integer> generateItensInHand(ArrayList<Boolean> selectedOrders, List<Boolean> selectedAisles){
        ArrayList<Integer> inHandItens = new ArrayList<>();

        for(int i = 0; i < nItems; i++){
            inHandItens.add(0);
        }

        for(int i = 0 ; i < nAisles; i++){
            if(selectedAisles.get(i)) calcAmountOfItens(aisles.get(i), inHandItens);
        }

        for(int i=0;i<nOrders; i++){
            if(selectedOrders.get(i)){
                Map<Integer, Integer> order = orders.get(i);
                for(Map.Entry<Integer, Integer> entry : order.entrySet()){
                    int oldValue = inHandItens.get(entry.getKey());
                    inHandItens.set(entry.getKey(), oldValue - entry.getValue());
                }
            }
        }

        return inHandItens;
    }

    private void countItemsInCollection(List<Map<Integer, Integer>> collection, ArrayList<Integer> count){
        count.clear();
        for (Map<Integer, Integer> entity : collection) {
            int sum = 0;
            for (Map.Entry<Integer, Integer> entry : entity.entrySet()) {
                sum += entry.getValue();
            }
            count.add(sum);
        }
    }

    private void calcSumOfItemsTypes(ArrayList<Integer> items, List<Map<Integer, Integer>> target){

        for(Map<Integer, Integer> entity : target){
            for(Map.Entry<Integer, Integer> entry : entity.entrySet()){
                Integer oldValue = items.get(entry.getKey());
                items.set(entry.getKey(), oldValue + entry.getValue());
            }
        }
    }

    private void trimAisles(){
        for(Map<Integer, Integer> entity : aisles){
            for(Map.Entry<Integer, Integer> entry : entity.entrySet()){
                if(entry.getValue() > itemsInOrders.get(entry.getKey())){
                    entity.replace(entry.getKey(), entry.getValue(), itemsInOrders.get(entry.getKey()));
                }
            }
        }
    }

    private void initialize(){

        rng = new Random(123);

        nOrders = orders.size();
        nAisles = aisles.size();

        itemsInOrders = new ArrayList<>();
        itemsInAisles = new ArrayList<>();
        for(int i=0; i<nItems; i++){
            itemsInOrders.add(0);
            itemsInAisles.add(0);
        }

        calcSumOfItemsTypes(itemsInOrders, orders);
        trimAisles();
        calcSumOfItemsTypes(itemsInAisles, aisles);


        // Count items in orders and aisles
        orderItems = new ArrayList<>();
        aisleItems = new ArrayList<>();
        countItemsInCollection(orders, orderItems);
        countItemsInCollection(aisles, aisleItems);

        mostComumItemInAisles = 0;
        mostComumItemInOrders = 0;
        for(int i=0; i<nItems; i++){
            if(itemsInAisles.get(i) > itemsInAisles.get(mostComumItemInAisles)) mostComumItemInAisles = i;
            if(itemsInOrders.get(i) > itemsInOrders.get(mostComumItemInOrders)) mostComumItemInOrders = i;
        }
        ArrayList<Double> orderItemsD = new ArrayList<>();
        for (Integer item : orderItems) {
            orderItemsD.add(item.doubleValue());
        }
        sortedBySizeOrders = new ArrayList<>();
        for(int i=0; i<nOrders; i++){
            sortedBySizeOrders.add(i);
        }
        heapSort(sortedBySizeOrders,orderItemsD);

        bestAisleSelectionFound = new ArrayList<>();
        bestOrderSelectionFound = new ArrayList<>();

    }

    public void aislesRuleQtt(ArrayList<Double> aislesItemQttDouble){
        for(int i = 0; i < nAisles; i++){
            aislesItemQttDouble.add((double)aisleItems.get(i));
        }
    }

    private void solution4(ArrayList<Boolean> bestSelectedOrders, ArrayList<Boolean> bestSelectedAisles, StopWatch stopWatch){

        // Sort aisles by priority
        List<Integer> solutionAisles = new ArrayList<>();

        ArrayList<Boolean> booleanAisles = new ArrayList<>();

        ArrayList<Integer> aislesOrder = new ArrayList<>();

        for(int i = 0; i < nAisles; i++){
            aislesOrder.add(i);
        }

        ArrayList<Double> aislesPriority = new ArrayList<>();

        aislesRuleQtt(aislesPriority);

        heapSort(aislesOrder, aislesPriority);

        // Select best aisles until reach lower bound
        int selectedAislesItems = 0;
        int k=0;
        while(selectedAislesItems < waveSizeLB){
            solutionAisles.add(aislesOrder.get(k));
            selectedAislesItems += aisleItems.get(k++);
        }

        // Create e initialize selected orders bool vector, if selectedOrdersBool[x] is true then order x is selected
        ArrayList<Boolean> selectedOrdersBool = new ArrayList<>();

        for(int i=0; i<nOrders; i++){
            selectedOrdersBool.add(false);
        }
        for(int i=0; i<nAisles; i++){
            booleanAisles.add(false);
        }
        for(int i : solutionAisles){
            booleanAisles.set(i,true);
        }
        // Select orders based in selected aisles

        int selectedOrdersItems = meta_raps(selectedOrdersBool, generateItensInHand(selectedOrdersBool, booleanAisles), 1, 1, solutionAisles.size(), stopWatch, false);

        List<Integer> solutionOrders = new ArrayList<>();

        // Best found solution and its objective value
        ChallengeSolution bestSolution = null;
        double bestResult=0;

        // i starts where k stops in aisles sequence
        for(int i=k; i<nAisles; i++){
            // if the sum of items of selected orders already reach the lowerBound, verify if current solution is better than the best solution
            if(selectedOrdersItems > waveSizeLB){
                solutionOrders.clear();
                // Parse bool vector to selected orders
                for(int j=0; j<nOrders; j++){
                    if(selectedOrdersBool.get(j)) solutionOrders.add(j);
                }

                ChallengeSolution currentSolution = new ChallengeSolution(new HashSet<>(solutionOrders), new HashSet<>(solutionAisles));

                // If current solution is better than the best, change best solution
                if(computeObjectiveFunction(currentSolution) > bestResult){
                    bestSelectedAisles.clear();
                    bestSelectedAisles.addAll(booleanAisles);

                    bestSelectedOrders.clear();
                    bestSelectedOrders.addAll(selectedOrdersBool);
                    bestSolution = currentSolution;
                    bestResult = computeObjectiveFunction(currentSolution);
                    bestOrderSelectionFound.clear();
                    bestOrderSelectionFound.addAll(selectedOrdersBool);
                    bestAisleSelectionFound.clear();
                    bestAisleSelectionFound.addAll(booleanAisles);
                }
            }
            // Break if reached upper bound
            if(selectedOrdersItems == waveSizeUB && solutionAisles.size() == 1) break;

            // Add next aisle to current solution
            solutionAisles.add(aislesOrder.get(i));
            booleanAisles.set(aislesOrder.get(i),true);

            // Try to add more orders in current solution
            selectedOrdersItems += meta_raps(selectedOrdersBool, generateItensInHand(selectedOrdersBool, booleanAisles), 1, 1, solutionAisles.size(), stopWatch, false);
        }
    }

    public void rule1(ArrayList<Double> utilityRatio, ArrayList<Integer> capacities){
        utilityRatio.clear();
        for(int i=0; i<nOrders; ++i){
            double penalty = 0;
            Map<Integer, Integer> order = orders.get(i);
            for(Map.Entry<Integer, Integer> item : order.entrySet()){
                penalty += (double)item.getValue()/(double)capacities.get(item.getKey());
            }
            utilityRatio.add((double)orderItems.get(i)/penalty);
        }
    }

    public void rule2(ArrayList<Double> utilityRatio, ArrayList<Integer> availableOrders, ArrayList<Integer> availableItems, double weight1, double weight2, double weight3){
        ArrayList<Double> itemsInAvailableOrders = new ArrayList<>();
        for(int i=0; i<nItems; i++){
            itemsInAvailableOrders.add(0.0);
        }

        for(Integer ord : availableOrders){
            Map<Integer, Integer> order = orders.get(ord);
            for(Map.Entry<Integer, Integer> item : order.entrySet()){
                Double oldValue = itemsInAvailableOrders.get(item.getKey());
                itemsInAvailableOrders.set(item.getKey(), oldValue + item.getValue());
            }
        }

        for(int i=0; i<nItems; ++i){
            Double oldValue = itemsInAvailableOrders.get(i);
            if(availableItems.get(i)==0){
                itemsInAvailableOrders.set(i, 0.0);
            }else{
                itemsInAvailableOrders.set(i, oldValue/availableItems.get(i));
            }
        }

        for(int i=0; i<nOrders; ++i){
            Map<Integer, Integer> order = orders.get(i);
            double value1 = 0;
            double value2 = 0;
            for(Map.Entry<Integer, Integer> item : order.entrySet()){
                value1 += (double)item.getValue()*(double)availableItems.get(item.getKey());
                value2 += (double)item.getValue() * itemsInAvailableOrders.get(item.getKey());
            }
            value1 *= weight1;
            value2 *= weight2;
            double value3 = weight3 * orderItems.get(i);
            utilityRatio.add(value1+value2+value3);
        }
    }

    public int meta_raps(ArrayList<Boolean> selectedOrders, ArrayList<Integer> availableItems, double priority, double restriction, double nAislesSelected, StopWatch stopWatch, boolean doLocalSearch){
//        System.out.println("meta_raps");
        int itemsUBSpent = 0;

        if(selectedOrders.size() != nOrders) return 0;

        // utilityRatio[x] is pseudo value of order x
        ArrayList<Double> utilityRatio = new ArrayList<>();

        // Calculate utilityRatio
        rule1(utilityRatio, availableItems);

        // Orders to be selected
        ArrayList<Integer> availableOrders = new ArrayList<>();
        //
        for(int i=0; i<nOrders; ++i){
            if(selectedOrders.get(i)){
                itemsUBSpent += orderItems.get(i);
            }else if(utilityRatio.get(i) > 0){
                availableOrders.add(i);
            }
        }

        int initialItemsSelected = itemsUBSpent;

        utilityRatio.clear();
        rule2(utilityRatio, availableOrders, availableItems, 1, 1, 1);

        heapSort(availableOrders, utilityRatio);
        double random01;
        int nbIter = availableOrders.size();
        for(int i=0; i<nbIter; ++i){

            int randomLala = rng.nextInt();
            random01 = Math.abs(randomLala/(double)Integer.MAX_VALUE);
            int idxSelectedOrder;
            if(random01 <= priority || availableOrders.size() == 1){
                idxSelectedOrder = 0;
            }else{
                int j=0;
                while(j < availableOrders.size() && utilityRatio.get(availableOrders.get(j)) > restriction*utilityRatio.get(0)){
                    ++j;
                }
                idxSelectedOrder = j > 1 ? rng.nextInt(j-1) : 0;
            }
            int selectedOrder = availableOrders.get(idxSelectedOrder);

            boolean canSelect = true;
            int orderSize = 0;
            Map<Integer, Integer> order = orders.get(selectedOrder);
            for(Map.Entry<Integer, Integer> item : order.entrySet()){
                if(item.getValue() > availableItems.get(item.getKey())){
                    canSelect = false;
                }
                orderSize += item.getValue();
            }
            if(orderSize > waveSizeUB - itemsUBSpent) canSelect = false;

            if(canSelect){
                for(Map.Entry<Integer, Integer> item : order.entrySet()){
                    Integer oldValue = availableItems.get(item.getKey());
                    availableItems.set(item.getKey(), oldValue - item.getValue());
                }
                itemsUBSpent += orderSize;
                selectedOrders.set(selectedOrder, true);
            }
            availableOrders.remove(idxSelectedOrder);
        }

        if(doLocalSearch){
            ArrayList<Boolean> selectedOrdersCur = new ArrayList<>(selectedOrders);
            ArrayList<Integer> availableItemsCur = new ArrayList<>(availableItems);
            int nIters = 0;
            int bestObj = itemsUBSpent;
            int curObj = itemsUBSpent;
            while (nIters < (nOrders) && stopWatch.getTime(TimeUnit.MILLISECONDS) < 590000) {

                int i = rng.nextInt(nOrders);
                int j = rng.nextInt(nOrders);
                if (i == j) j = (j + 1) % nOrders;

                int itemsRemoved = selectedOrdersCur.get(i) ? orderItems.get(i) : 0;
                boolean swapOccurred = swapOrderNeighbour(availableItemsCur, selectedOrdersCur, curObj, i, j);

                if (swapOccurred && bestObj < (curObj - itemsRemoved + orderItems.get(j))) {
                    bestObj = curObj - itemsRemoved + orderItems.get(j);
                    curObj = bestObj;
                    selectedOrders.clear();
                    selectedOrders.addAll(selectedOrdersCur);
                    availableItems.clear();
                    availableItems.addAll(availableItemsCur);
                } else {
                    selectedOrdersCur.clear();
                    selectedOrdersCur.addAll(selectedOrders);
                    availableItemsCur.clear();
                    availableItemsCur.addAll(availableItems);
                }
                nIters++;
            }
        }
        int finalItemsSelected = 0;
        for(int k=0; k < nOrders; k++){
            if(selectedOrders.get(k)){
                finalItemsSelected += orderItems.get(k);
            }
        }

        return finalItemsSelected - initialItemsSelected;
    }

    public boolean canAddOrder(ArrayList<Integer> availableItems, int itemsUBSpent, int orderIdx){
        boolean canSelect = true;
        int orderSize = 0;
        Map<Integer, Integer> order = orders.get(orderIdx);
        for(Map.Entry<Integer, Integer> item : order.entrySet()){
            if(item.getValue() > availableItems.get(item.getKey())){
                canSelect = false;
            }
            orderSize += item.getValue();
        }
        if(orderSize + itemsUBSpent > waveSizeUB) canSelect = false;
        return canSelect;
    }

    public boolean swapOrderNeighbour(ArrayList<Integer> availableItems, ArrayList<Boolean> selectedOrders, int itemsUBSpent, int i, int j){
        Map<Integer, Integer> orderI = orders.get(i);
        Map<Integer, Integer> orderJ = orders.get(j);

        if(selectedOrders.get(i)){
            selectedOrders.set(i, false);
            for(Map.Entry<Integer, Integer> item : orderI.entrySet()){
                Integer oldValue = availableItems.get(item.getKey());
                availableItems.set(item.getKey(), oldValue + item.getValue());
            }
            itemsUBSpent -= orderItems.get(i);
        }

        if(!selectedOrders.get(j) && itemsUBSpent + orderItems.get(j) > waveSizeLB && canAddOrder(availableItems, itemsUBSpent, j)){
            selectedOrders.set(j, true);
            for(Map.Entry<Integer, Integer> item : orderJ.entrySet()){
                Integer oldValue = availableItems.get(item.getKey());
                availableItems.set(item.getKey(), oldValue - item.getValue());
            }
            return true;
        }

        return false;
    }

    public int swapOrderBestNeighbour(ArrayList<Integer> availableItems, ArrayList<Boolean> selectedOrders, double nAislesSelected){
        int itemsUBSpent = 0;
        for(int i=0; i<nOrders; ++i){
            if(selectedOrders.get(i)){
                itemsUBSpent += orderItems.get(i);
            }
        }
        int bestObj = itemsUBSpent;

        ArrayList<Boolean> selectedOrdersCpy = new ArrayList<>(selectedOrders);
        ArrayList<Integer> availableItemsCpy = new ArrayList<>(availableItems);
        ArrayList<Integer> availableItemsCur = new ArrayList<>();

        for(int i = 0; i < nOrders; ++i){
            for(int j = 0; j < nOrders; ++j){

                int itemsRemoved = selectedOrdersCpy.get(i) ? orderItems.get(i) : 0;
                availableItemsCur.addAll(availableItemsCpy);

                boolean orderAdded = swapOrderNeighbour(availableItemsCur, selectedOrdersCpy, itemsUBSpent, i, j);
                if(orderAdded && bestObj < (itemsUBSpent - itemsRemoved + orderItems.get(j))){
                    bestObj =  itemsUBSpent - itemsRemoved + orderItems.get(j);
                    selectedOrders.set(i, false);
                    selectedOrdersCpy.set(j, true);
                    availableItems.clear();
                    availableItems.addAll(availableItemsCur);
                }

                if(itemsRemoved > 0){
                    selectedOrdersCpy.set(i, true);
                }
                if(orderAdded){
                    selectedOrdersCpy.set(j, false);
                }
                availableItemsCur.clear();
            }
        }

        return bestObj;
    }

    public void removeOrderNeighbour(ArrayList<Boolean> selectedAisles, ArrayList<Boolean> selectedOrders, int i ){
        if (selectedOrders.get(i)) selectedOrders.set(i, false);
        else return;
        
        ArrayList<Integer> inHandItens = generateItensInHand(selectedOrders,selectedAisles);
        int itemsUBSpent = 0;
        for(int o = 0; o < nOrders; ++o){
            if(selectedOrders.get(o)) itemsUBSpent += orderItems.get(o);
        }
        for(int o : sortedBySizeOrders){
            if(o != i && !selectedOrders.get(o)){
                boolean canSelect = true;
                int orderSize = 0;
                Map<Integer, Integer> order = orders.get(o);
                for(Map.Entry<Integer, Integer> item : order.entrySet()){
                    if(item.getValue() > inHandItens.get(item.getKey())){
                        canSelect = false;
                    }
                    orderSize += item.getValue();
                }
                if(orderSize > waveSizeUB - itemsUBSpent) canSelect = false;
                selectedOrders.set(o, canSelect);
                if(canSelect) break;
            }
        }
    }

    public  ArrayList<ArrayList<Boolean>>  removeOrderNeighborhood(ArrayList<Boolean> selectedAisles, ArrayList<Boolean> selectedOrders){
        ArrayList<ArrayList<Boolean>> neighborhood = new ArrayList<>();
        int nNeighbours = 0;
        for(int o = 0; o < nOrders; ++o){
            if(selectedOrders.get(o)){
                neighborhood.add(new ArrayList<Boolean>(selectedOrders));
                removeOrderNeighbour(selectedAisles,neighborhood.get(nNeighbours++),o);
            }
        }
        return neighborhood;
    }

    public int invertAisleStateNeighbour(ArrayList<Boolean> selectedAisles, ArrayList<Boolean> selectedOrders, int a, StopWatch stopWatch){
        if(selectedAisles.get(a)){
            selectedAisles.set(a, false);
        }else{
            selectedAisles.set(a, true);
        }
        int nAislesSelected = 0;
        for(int i=0; i< nAisles; ++i){
            if(selectedAisles.get(i)){
                nAislesSelected++;
            }
        }

        return meta_raps(selectedOrders, generateItensInHand(selectedOrders, selectedAisles), 1, 1, nAislesSelected, stopWatch, true);
    }

    public double invertAisleBestNeighbour(ArrayList<Boolean> selectedAisles, ArrayList<Boolean> selectedOrders, StopWatch stopWatch){
        ArrayList<Boolean> selectedAislesCopy = new ArrayList<>(selectedAisles);
        ArrayList<Boolean> selectedOrdersCopy = new ArrayList<>();
        for(int i=0; i<nOrders; ++i){
            selectedOrdersCopy.add(false);
        }

        ArrayList<Boolean> selectedAislesCur = new ArrayList<>();
        ArrayList<Boolean> selectedOrdersCur = new ArrayList<>();

        double bestObj = computeObjectiveFunction(new ChallengeSolution(new HashSet<>(boolToInt(selectedOrders)), new HashSet<>(boolToInt(selectedAisles))));
        for(int o = 0; o < nAisles; ++o){
            selectedAislesCur.addAll(selectedAislesCopy);
            selectedOrdersCur.addAll(selectedOrdersCopy);
            int itemsAdded = invertAisleStateNeighbour(selectedAislesCur, selectedOrdersCur, o, stopWatch);
            if(itemsAdded < waveSizeLB || itemsAdded > waveSizeUB) continue;
            double curObj = computeObjectiveFunction(new ChallengeSolution(new HashSet<>(boolToInt(selectedOrdersCur)), new HashSet<>(boolToInt(selectedAislesCur))));
            if(curObj > bestObj){
                bestObj = curObj;
                selectedAisles.clear();
                selectedAisles.addAll(selectedAislesCur);
                selectedOrders.clear();
                selectedOrders.addAll(selectedOrdersCur);
            }
            selectedAislesCur.clear();
            selectedOrdersCur.clear();
            if(stopWatch.getTime(TimeUnit.MILLISECONDS) > 590000) break;
        }

        return bestObj;
    }

    public ArrayList<Integer> boolToInt(ArrayList<Boolean> bools){
        ArrayList<Integer> ints = new ArrayList<>();
        for(int i = 0 ;i < bools.size(); ++i){
            if(bools.get(i)) ints.add(i);
        }
        return ints;
    }

    public void LocalSearchBestImp(ArrayList<Boolean> selectedAisles, ArrayList<Boolean> selectedOrders, StopWatch stopWatch){
        double bestObj = computeObjectiveFunction(new ChallengeSolution(new HashSet<>(boolToInt(selectedOrders)), new HashSet<>(boolToInt(selectedAisles))));

        final ArrayList<Boolean> selectedAislesCopy = new ArrayList<>(selectedAisles);
        final ArrayList<Boolean> selectedOrdersCopy = new ArrayList<>(selectedOrders);

        boolean reachMinimum = false;
        while(!reachMinimum && stopWatch.getTime(TimeUnit.MILLISECONDS) < 590000){
            reachMinimum = true;
            double curObj = invertAisleBestNeighbour(selectedAislesCopy, selectedOrdersCopy, stopWatch);
            if(curObj > bestObj){
                bestObj = curObj;
                reachMinimum = false;
                selectedAisles.clear();
                selectedAisles.addAll(selectedAislesCopy);
                selectedOrders.clear();
                selectedOrders.addAll(selectedOrdersCopy);
                bestAisleSelectionFound.clear();
                bestAisleSelectionFound.addAll(selectedAislesCopy);
                bestOrderSelectionFound.clear();
                bestOrderSelectionFound.addAll(selectedOrdersCopy);
            }
        }
    }

    public ChallengeSolution solve(StopWatch stopWatch) {
        ArrayList<Boolean> o = new ArrayList<>(), a = new ArrayList<>();

        solution4(o, a, stopWatch);
        LocalSearchBestImp(a, o, stopWatch);
        System.out.println(stopWatch.getTime(TimeUnit.MILLISECONDS));
        return new ChallengeSolution(new HashSet<>(boolToInt(bestOrderSelectionFound)), new HashSet<>(boolToInt(bestAisleSelectionFound)));
    }


    /*
     * Get the remaining time in seconds
     */
    protected long getRemainingTime(StopWatch stopWatch) {
        return Math.max(
                TimeUnit.SECONDS.convert(MAX_RUNTIME - stopWatch.getTime(TimeUnit.MILLISECONDS), TimeUnit.MILLISECONDS),
                0);
    }

    protected boolean isSolutionFeasible(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return false;
        }

        int[] totalUnitsPicked = new int[nItems];
        int[] totalUnitsAvailable = new int[nItems];

        // Calculate total units picked
        for (int order : selectedOrders) {
            for (Map.Entry<Integer, Integer> entry : orders.get(order).entrySet()) {
                totalUnitsPicked[entry.getKey()] += entry.getValue();
            }
        }

        // Calculate total units available
        for (int aisle : visitedAisles) {
            for (Map.Entry<Integer, Integer> entry : aisles.get(aisle).entrySet()) {
                totalUnitsAvailable[entry.getKey()] += entry.getValue();
            }
        }

        // Check if the total units picked are within bounds
        int totalUnits = Arrays.stream(totalUnitsPicked).sum();
        if (totalUnits < waveSizeLB || totalUnits > waveSizeUB) {
            return false;
        }

        // Check if the units picked do not exceed the units available
        for (int i = 0; i < nItems; i++) {
            if (totalUnitsPicked[i] > totalUnitsAvailable[i]) {
                return false;
            }
        }

        return true;
    }

    protected double computeObjectiveFunction(ChallengeSolution challengeSolution) {
        Set<Integer> selectedOrders = challengeSolution.orders();
        Set<Integer> visitedAisles = challengeSolution.aisles();
        if (selectedOrders == null || visitedAisles == null || selectedOrders.isEmpty() || visitedAisles.isEmpty()) {
            return 0.0;
        }
        int totalUnitsPicked = 0;

        // Calculate total units picked
        for (int order : selectedOrders) {
            totalUnitsPicked += orders.get(order).values().stream()
                    .mapToInt(Integer::intValue)
                    .sum();
        }

        // Calculate the number of visited aisles
        int numVisitedAisles = visitedAisles.size();

        // Objective function: total units picked / number of visited aisles
        return (double) totalUnitsPicked / numVisitedAisles;
    }
}
