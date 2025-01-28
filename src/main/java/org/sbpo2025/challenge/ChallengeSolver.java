package org.sbpo2025.challenge;

import org.apache.commons.lang3.time.StopWatch;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class ChallengeSolver {

    private final long MAX_RUNTIME = 600000; // milliseconds; 10 minutes

    protected List<Map<Integer, Integer>> orders;
    protected List<Map<Integer, Integer>> aisles;
    protected int nItems;
    protected int waveSizeLB;
    protected int waveSizeUB;

    //nossas variáveis
    protected Map.Entry<Integer,Integer>[] totalUnitsAisles;
    protected Map.Entry<Integer,Integer>[] totalUnitsOrders;

    List<Map<Integer,Integer>> sOrders;
    List<Map<Integer,Integer>> sAisles;

    int[] amountItensAisles;
    int[] amountItensOrders;
    int mostComumItem;

    public ChallengeSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        this.totalUnitsAisles = new Map.Entry[aisles.size()];
        this.totalUnitsOrders = new Map.Entry[orders.size()];
        sOrders = new ArrayList<>(orders);
        sAisles = new ArrayList<>(aisles);
        amountItensAisles = new int[nItems];
        amountItensOrders = new int[nItems];
        initialize();
    }
    // calcula a soma da quantidade de itens em um pedido ou corredor
    public void fillTotalUnits(List<Map<Integer, Integer>> arr, Map.Entry<Integer,Integer>[] units){
        int n = arr.size();
        for(int i = 0; i < n; i++) {
            int sum = 0;
            for(Map.Entry<Integer, Integer> entry : arr.get(i).entrySet()) {
                sum += entry.getValue();
            }
            units[i] = new AbstractMap.SimpleEntry<>(i, sum);
        }
    }
    // orderna corredores e pedidos baseados na quantidade de itens
    private void heapfy(List<Map<Integer, Integer>> arr, int n, int i, Map.Entry<Integer,Integer>[] units){
        int min = i;
        int l = 2 * i +1;
        int r = 2 * i + 2;
        if(l < n && units[l].getValue() < units[min].getValue()) min = l;
        if(r < n && units[r].getValue() < units[min].getValue()) min = r;

       if(min != i){
           Map<Integer, Integer> temp = arr.get(i);
           arr.set(i, arr.get(min));
           arr.set(min, temp);
           Map.Entry<Integer,Integer> tempUnits = units[i];
           units[i] = units[min];
           units[min] = tempUnits;
           heapfy(arr, n, min, units);
       }
    }
    private void heapSort(List<Map<Integer, Integer>> arr,Map.Entry<Integer,Integer>[] units){
        int n = arr.size();
        for(int i = n/2-1; i >= 0; i--){
            heapfy(arr, n, i, units);
        }
        for(int i = n-1; i >= 0; i--){
            Map<Integer, Integer> temp = arr.get(0);
            arr.set(0, arr.get(i));
            arr.set(i, temp);
            Map.Entry<Integer,Integer> tempUnits = units[0];
            units[0] = units[i];
            units[i] = tempUnits;
            heapfy(arr, i, 0, units);
        }
    }
    // calcula o total dos itens idexados pelo id em um corredor ou pedido
    void calcAmountOfItens(Map<Integer, Integer> thing,int[] amount){
        for(Map.Entry<Integer, Integer> entry : thing.entrySet()){
            amount[entry.getKey()] += entry.getValue();
        }
    }
    // calcula o total dos itens idexados pelo id em todos os corredores ou pedidos
    void calcTotalAmountOfItems(int[] amount, List<Map<Integer, Integer>> arr){
        for(Map<Integer, Integer> thing : arr){
            calcAmountOfItens(thing, amount);
        }
    }

    int [] generateItensInHand(List<Integer> selectedAisles){
        int [] inHandItens = new int[nItems];
        for(int i : selectedAisles){
            calcAmountOfItens(aisles.get(i), inHandItens);
        }
        return inHandItens
    }

    List<Integer> selectOrdersByAisles(List<Integer> selectedAisles){
        // faz o calculo do total de itens presentes nestes corredores
        int[] inHandItens = generateItensInHand(selectedAisles);


        List<Integer> selectedOrders = new ArrayList<>();
        // percorre os pedidos do com maior quantidade até o menor
        for(int i = 0; i < sOrders.size(); i++){
            int[] auxItens = new int[nItems];
            Boolean acept = true;
            // percorre os itens do pedido
            for(Map.Entry<Integer, Integer> entry : sOrders.get(i).entrySet()){
                //confere se podemos atenter aquele item
                if(inHandItens[entry.getKey()] >= entry.getValue()){
                    calcAmountOfItens(orders.get(i), auxItens);
                } else acept = false; // caso um item não puder ser atendido
            }
            // se todos os pedidos forem atenditos
            if(acept){
                // pedido adicionado a solução
                selectedOrders.add(totalUnitsOrders[i].getKey());
                // diminui os itens do pedido dos itens que temos nos corredores
                for(int j = 0; j < nItems; j++){
                    inHandItens[j] -= auxItens[j];
                }
            }
            for(int j = 0; j < nItems; j++){
                auxItens[j] = 0;
            }
        }
        return selectedOrders;
    }
    private void initialize(){
        //calcula a quantidade de itens em cada corredor e pedido
        fillTotalUnits(orders, totalUnitsOrders);
        fillTotalUnits(aisles, totalUnitsAisles);

        //ordena pedidos e corredores pela quantidade de itens presentes e salva em uma copia do vetor original
        heapSort(sOrders, totalUnitsOrders);
        heapSort(sAisles, totalUnitsAisles);

        //calcular a quantidade total dos itens nos corredores e pedidos
        calcTotalAmountOfItems(amountItensAisles, aisles);
        calcTotalAmountOfItems(amountItensOrders,orders);

        //calcular o item que com maior presença nos pedidos
        mostComumItem = 0;
        for(int i = 1; i < nItems; i++){
            if(amountItensOrders[i] > amountItensOrders[mostComumItem]) mostComumItem = i;
        }

    }

    private ChallengeSolution initialSolution01(){
        List<Integer> solution = new ArrayList<>();
        int bound = 0;

        //adicione corredores na solução de acord com a limitação de UB
        for(int i = 0; i < sAisles.size(); i++){
            if(bound + totalUnitsAisles[i].getValue() <= waveSizeUB){
                bound += totalUnitsAisles[i].getValue();
                solution.add(totalUnitsAisles[i].getKey());
            }
        }

        List<Integer> solution selectOrdersByAisles(solution);
        return new ChallengeSolution(new HashSet<>(solution), new HashSet<>(solution2));
    }

    public ChallengeSolution solve(StopWatch stopWatch) {

        return initialSolution01();
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
