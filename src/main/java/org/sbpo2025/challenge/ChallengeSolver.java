package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.awt.geom.Arc2D;
import java.lang.reflect.Array;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

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
    protected ArrayList<Integer> itemsInAisles;              // itemsInOrders[x] is number of items x in all aisles

    //nossas variáveis
//    protected int[] sumTotalUnitsAisle;
//    protected int[] sumTotalUnitsOrder;
    //Guarda a soma total de itens em uma determinada corredor/pedido bem como o index da corredor/pedido no vetor original
    protected Map.Entry<Integer,Integer>[] sumSTotalUnitsAisle;
    protected Map.Entry<Integer,Integer>[] sumSTotalUnitsOrder;
    //List ordenado pela quantidade de itens em cada corredor/pedido
//    List<Map<Integer,Integer>> sOrders;
//    List<Map<Integer,Integer>> sAisles;

    //vetor com a quantidade de cada item em todos oc corredores/pedidos
//    int[] amountItensAisles;
//    int[] amountItensOrders;

    //item mais comum nos pedidos
    int mostComumItemInOrders;
    int mostComumItemInAisles;

    private static Map<String, List<Integer>> memo;

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

/*
    // calcula a soma da quantidade de itens em um pedido ou corredor
    public void sumItens(List<Map<Integer, Integer>> arr, Map.Entry<Integer,Integer>[] units,int[] sumV){
        int n = arr.size();
        for(int i = 0; i < n; i++) {
            int sum = 0;
            for(Map.Entry<Integer, Integer> entry : arr.get(i).entrySet()) {
                sum += entry.getValue();
            }
            sumV[i] = sum;
            units[i] = new AbstractMap.SimpleEntry<>(i, sum);
        }
    }

    public void qtdMItens(List<Map<Integer, Integer>> arr, Map.Entry<Integer,Integer>[] units,int[] sumV){
        int n = arr.size();
        for(int i = 0; i < n; i++) {
            int sum = 0;
            for(Map.Entry<Integer, Integer> entry : arr.get(i).entrySet()) {
                if(entry.getKey() == mostComumItem ) sum = entry.getValue();

            }
            units[i] = new AbstractMap.SimpleEntry<>(i, sum);
        }
    }
*/


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
/*
    // calcula o total dos itens idexados pelo id em todos os corredores ou pedidos
    void calcTotalAmountOfItems(ArrayList<Integer> amount, List<Map<Integer, Integer>> arr){
        for(Map<Integer, Integer> thing : arr){
            calcAmountOfItens(thing, amount);
        }
    }

 */

    // calcula a quantidade de itens indexados pelo id nos corredores selecionados
    ArrayList<Integer> generateItensInHand(ArrayList<Boolean> selectedOrders, List<Integer> selectedAisles){
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

    private void initialize(){

        nOrders = orders.size();
        nAisles = aisles.size();

        // Count items in orders and aisles
        orderItems = new ArrayList<>();
        aisleItems = new ArrayList<>();
        countItemsInCollection(orders, orderItems);
        countItemsInCollection(aisles, aisleItems);

        itemsInOrders = new ArrayList<>();
        itemsInAisles = new ArrayList<>();
        for(int i=0; i<nItems; i++){
            itemsInOrders.add(0);
            itemsInAisles.add(0);
        }

        calcSumOfItemsTypes(itemsInOrders, aisles);
        calcSumOfItemsTypes(itemsInAisles, aisles);

        mostComumItemInAisles = 0;
        mostComumItemInOrders = 0;
        for(int i=0; i<nItems; i++){
            if(itemsInAisles.get(i) > itemsInAisles.get(mostComumItemInAisles)) mostComumItemInAisles = i;
            if(itemsInOrders.get(i) > itemsInOrders.get(mostComumItemInOrders)) mostComumItemInOrders = i;
        }

       // memo = new HashMap<>();
        //calcular o item que com maior presença nos pedidos

    }

/*
    //    private ChallengeSolution initialSolution01(){
    //        List<Integer> solution = new ArrayList<>();
    //        int bound = 0;
    //
    //        //adicione corredores na solução de acord com a limitação de UB
    //        for(int i = 0; i < sAisles.size(); i++){
    //            if(bound +sumTotalUnitsAisle[sumSTotalUnitsAisle[i].getKey()]  <= waveSizeUB){
    //                bound += sumTotalUnitsAisle[sumSTotalUnitsAisle[i].getKey()];
    //                solution.add(sumSTotalUnitsAisle[i].getKey());
    //            }
    //        }
    //
    //        List<Integer> solution2 =  selectOrdersByAisles(solution);
    //        bound = 0;
    //
    //        for(int i : solution2) bound += sumSTotalUnitsOrder[i].getValue();
    //        while(bound < waveSizeLB && solution.size() < sAisles.size()){
    //            bound = 0;
    //            solution.add(sumSTotalUnitsAisle[solution.size()].getKey());
    //            solution2 =  selectOrdersByAisles(solution);
    //            for(int i : solution2) bound += sumSTotalUnitsOrder[i].getValue();
    //        }
    //        if(bound > waveSizeUB ) System.out.println("deu caca");
    //        return new ChallengeSolution(new HashSet<>(solution2), new HashSet<>(solution));  //sol2 sol2 ???
    //    }

    //    private ChallengeSolution initialSolution02(){
    //
    //        // Guardar a melhor solução
    //        List<Integer> bSelectedAisles = new ArrayList<>();
    //        List<Integer> bSelectedOrders = new ArrayList<>();
    //        double bestQuality = -1;
    //        //gerar solução valida inicial
    //        for(int i = 0; i < sAisles.size(); i++){
    //            bSelectedAisles.add(sumSTotalUnitsAisle[i].getKey());
    //        }
    //        bSelectedOrders = selectOrdersByAisles(bSelectedAisles);
    //        bestQuality = calcSolutionBound(bSelectedOrders)/bSelectedAisles.size();
    //        if(calcSolutionBound(bSelectedOrders) < waveSizeLB) bSelectedOrders = selectReverseOrdersByAisles(bSelectedAisles);
    //        // vetores auxiliates
    //        List<Integer> selectedOrdersAux = new ArrayList<>();
    //        List<Integer> selectedAislesAux = new ArrayList<>();
    //        List<Integer> selectedROrdersAux = new ArrayList<>();
    //        // bound da solução atual
    //        int bound = 0;
    //
    //        for(int i = 0; i < sAisles.size(); i++){
    //            selectedAislesAux.add(sumSTotalUnitsAisle[i].getKey());
    //            if(bound + sumTotalUnitsAisle[sumSTotalUnitsAisle[i].getKey()] >= waveSizeLB){
    //                if(bound > waveSizeLB) break;
    //                selectedOrdersAux = selectOrdersByAisles(selectedAislesAux);
    //                selectedROrdersAux = selectReverseOrdersByAisles(selectedAislesAux);
    //                double auxRbound = calcSolutionBound(selectedROrdersAux);
    //                double qualityR = auxRbound / selectedAislesAux.size();
    //                double auxbound = calcSolutionBound(selectedOrdersAux);
    //                double quality = auxbound / selectedAislesAux.size();
    //                if(quality < qualityR && auxRbound >= waveSizeLB){
    //                    quality = qualityR;
    //                    auxbound = auxRbound;
    //                    selectedOrdersAux = selectedROrdersAux;
    //                }
    //                if(quality > bestQuality && auxbound >= waveSizeLB){
    //                    bestQuality = quality;
    //                    bSelectedAisles = selectedAislesAux;
    //                    bSelectedOrders = selectedOrdersAux;
    //
    //                }
    //            }
    //        }
    //        if(selectedAislesAux.isEmpty()) return initialSolution01();
    //        return new ChallengeSolution(new HashSet<>(bSelectedOrders), new HashSet<>(bSelectedAisles));
    //    }

        private List<Integer> melhorSolução(List<Integer> s1, List<Integer> s2){
            if(calcSolutionBound(s1) > calcSolutionBound(s2)) return s1;
            else return s2;
        }

    //    private boolean podeAtender(int k, int [] estoque){
    //        int [] estoqueAux = new int[estoque.length];
    //        calcAmountOfItens(orders.get(k), estoqueAux);
    //
    //        for(int i = 0; i <nItems;i++){
    //            if(estoqueAux[i] > estoque[i]){
    //                return false;
    //            }
    //        }
    //        for(int i = 0; i <nItems; i++){
    //            estoque[i] -= estoqueAux[i];
    //        }
    //        return true;
    //    }

    //    List<Integer> F(int k, int valAlvo, int[] estoque){
    //        if(k < 0 || valAlvo < 0) return new ArrayList<>(); // pedidos zerados ou meta atingida
    //
    //        //chave de memorização
    //
    //
    //        // não incluir pedido atual
    //        List<Integer> aux1 = F(k-1, valAlvo, estoque);
    //        List<Integer> aux2 = new ArrayList<>();
    //        int valk = sumTotalUnitsOrder[k];
    //        int [] auxEstoque = Arrays.copyOf(estoque, estoque.length);
    //        if(podeAtender(k,auxEstoque)){
    //            aux2 = new ArrayList<>(F(k-1, valAlvo-valk, auxEstoque));
    //            aux2.add(k);
    //        }
    //
    //        return melhorSolução(aux1, aux2);
    //    }

    //    public  List<Integer> fI(int valAlvo, int[] estoque) {
    //        int n = nOrders;
    //        List<Integer>[] dp = new ArrayList[valAlvo + 1];
    //        dp[0] = new ArrayList<>();
    //
    //        for (int k = 0; k < n; k++) {
    //            int valk = sumTotalUnitsOrder[k];
    //            int[] auxEstoque = Arrays.copyOf(estoque, estoque.length);
    //            if (podeAtender(k, auxEstoque)) {
    //                for (int j = valAlvo; j >= valk; j--) {
    //                    if (dp[j - valk] != null) {
    //                        List<Integer> novaLista = new ArrayList<>(dp[j - valk]);
    //                        novaLista.add(k);
    //                        if (dp[j] == null || melhorSolução(dp[j], novaLista) == novaLista) {
    //                            dp[j] = novaLista;
    //                        }
    //                    }
    //                }
    //            }
    //        }
    //
    //        for (int i = valAlvo; i >= 0; i--) {
    //            if (dp[i] != null) {
    //                return dp[i];
    //            }
    //        }
    //        return new ArrayList<>();
    //    }

    //    private ChallengeSolution solution3(){
    //        int bound = 0;
    //        List<Integer> solutionAisles = new ArrayList<>();
    //        for(int i = 0; i < sAisles.size(); i++){
    //            bound += sumTotalUnitsAisle[sumSTotalUnitsAisle[i].getKey()];
    //            if(bound > waveSizeUB) break;
    //            else if(bound > waveSizeLB + (waveSizeLB/2)) break;
    //            else{
    //                solutionAisles.add(sumSTotalUnitsAisle[i].getKey());
    //            }
    //        }
    //        List<Integer> solutionOrders = fI(waveSizeUB,generateItensInHand(solutionAisles));
    //        return new ChallengeSolution(new HashSet<>(solutionOrders), new HashSet<>(solutionAisles));
    //    }
*/

    public void aislesRuleQtt(ArrayList<Double> aislesItemQttDouble){
        for(int i = 0; i < nAisles; i++){
            aislesItemQttDouble.add((double)aisleItems.get(i));
        }
    }

    public void aislesRuleMostComumItem(ArrayList<Double> mostComumItemQttDouble){
        int i=0;
        mostComumItemQttDouble.clear();
        for(Map<Integer, Integer> aisle : aisles){
            mostComumItemQttDouble.add(0.0);
            for(Map.Entry<Integer, Integer> entry : aisle.entrySet()){
                if(entry.getKey() == mostComumItemInOrders){
                    mostComumItemQttDouble.set(i, (double)entry.getValue());
                }
            }
            ++i;
        }
    }

    private ChallengeSolution solution4(){

        Random rng = new Random(123);

        // Sort aisles by priority
        List<Integer> solutionAisles = new ArrayList<>();

        ArrayList<Integer> aislesOrder = new ArrayList<>();

        for(int i = 0; i < nAisles; i++){
            aislesOrder.add(i);
        }

        ArrayList<Double> aislesPriority = new ArrayList<>();

//        aislesRuleQtt(aislesPriority);
        aislesRuleMostComumItem(aislesPriority);

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

        // Select orders based in selected aisles
        int selectedOrdersItems = meta_raps(selectedOrdersBool, generateItensInHand(selectedOrdersBool, solutionAisles), 1, 1, 5, rng);

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
                    bestSolution = currentSolution;
                    bestResult = computeObjectiveFunction(currentSolution);
                }
            }
            // Break if reached optimum
            if(selectedOrdersItems == waveSizeUB) break;

            // Add next aisle to current solution
            solutionAisles.add(aislesOrder.get(i));

            // Try to add more orders in current solution
            selectedOrdersItems += meta_raps(selectedOrdersBool, generateItensInHand(selectedOrdersBool, solutionAisles), 0.8, 0.5,  5, rng);
        }

        return bestSolution;
    }

    public void rule1(ArrayList<Double> utilityRatio, ArrayList<Integer> capacities){
        utilityRatio.clear();
        for(int i=0; i<nOrders; ++i){
            double penalty = 0;
            Map<Integer, Integer> order = orders.get(i);
            for(Map.Entry<Integer, Integer> item : order.entrySet()){
//                System.out.println((double)item.getValue() + " " + (double)capacities.get(item.getKey()));
                penalty += (double)item.getValue()/(double)capacities.get(item.getKey());
            }
            utilityRatio.add((double)orderItems.get(i)/penalty);
        }
    }

    public void rule2(ArrayList<Double> utilityRatio){
        for(int i=0; i<nOrders; ++i){
            
        }
    }

    public int meta_raps(ArrayList<Boolean> selectedOrders, ArrayList<Integer> availableItems, double priority, double restriction, int nb_iterations, Random rng){

        int itemsAdded = 0;
        int itemsUBSpent = 0;

        if(selectedOrders.size() != nOrders) return 0;

        // utilityRatio[x] is pseudo value of order x
        ArrayList<Double> utilityRatio = new ArrayList<>();

        // Calculate utilityRatio
        rule1(utilityRatio, availableItems);
//        System.out.println(availableItems);

        // Orders to be selected
        ArrayList<Integer> availableOrders = new ArrayList<>();

        //
        for(int i=0; i<nOrders; ++i){
            if(selectedOrders.get(i)){
                Map<Integer, Integer> order = orders.get(i);
                for(Map.Entry<Integer, Integer> item : order.entrySet()){
                    itemsUBSpent += item.getValue();
                }
            }else if(utilityRatio.get(i) > 0){
                availableOrders.add(i);
            }
        }

        heapSort(availableOrders, utilityRatio);
//        System.out.println(penaltyFactor);
//        System.out.println(availableOrders);
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
                itemsAdded += orderSize;
                selectedOrders.set(selectedOrder, true);
//                rule1(penaltyFactor, utilityRatio);
//                heapSort(availableOrders, utilityRatio);
            }
            availableOrders.remove(idxSelectedOrder);
        }

       return itemsAdded;
    }

    public ChallengeSolution solve(StopWatch stopWatch) {

        return solution4();
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
