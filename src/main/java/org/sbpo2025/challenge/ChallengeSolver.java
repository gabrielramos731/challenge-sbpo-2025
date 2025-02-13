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

    //nossas variáveis
    protected int[] sumTotalUnitsAisle;
    protected int[] sumTotalUnitsOrder;
    //Guarda a soma total de itens em uma determinada corredor/pedido bem como o index da corredor/pedido no vetor original
    protected Map.Entry<Integer,Integer>[] sumSTotalUnitsAisle;
    protected Map.Entry<Integer,Integer>[] sumSTotalUnitsOrder;
    //List ordenado pela quantidade de itens em cada corredor/pedido
    List<Map<Integer,Integer>> sOrders;
    List<Map<Integer,Integer>> sAisles;

    //vetor com a quantidade de cada item em todos oc corredores/pedidos
    int[] amountItensAisles;
    int[] amountItensOrders;

    //item mais comum nos pedidos
    int mostComumItem;

    private static Map<String, List<Integer>> memo;

    public ChallengeSolver(List<Map<Integer, Integer>> orders, List<Map<Integer, Integer>> aisles, int nItems, int waveSizeLB, int waveSizeUB) {
        this.orders = orders;
        this.aisles = aisles;
        this.nItems = nItems;
        this.waveSizeLB = waveSizeLB;
        this.waveSizeUB = waveSizeUB;
        this.sumSTotalUnitsAisle = new Map.Entry[aisles.size()];
        this.sumSTotalUnitsOrder = new Map.Entry[orders.size()];
        sumTotalUnitsAisle = new int[aisles.size()];
        sumTotalUnitsOrder = new int[orders.size()];
        sOrders = new ArrayList<>(orders);
        sAisles = new ArrayList<>(aisles);
        amountItensAisles = new int[nItems];
        amountItensOrders = new int[nItems];
        initialize();
    }
    int sum(int[] vetor, int n){
        int sum = 0;
        for(int i = 0; i < n; i++){
            sum += vetor[i];
        }
        return sum;
    }
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

    // calcula a quantidade de itens indexados pelo id nos corredores selecionados
    int [] generateItensInHand(List<Integer> selectedAisles){
        int [] inHandItens = new int[nItems];
        for(int i : selectedAisles){
            calcAmountOfItens(aisles.get(i), inHandItens);
        }

        return inHandItens;
    }

    // retorna os pedidos aceitos por id com base nos corredores selecionados
    List<Integer> selectOrdersByAisles(List<Integer> selectedAisles){
        int bound = 0;
        // faz o calculo do total de itens presentes nestes corredores
        int[] inHandItens = generateItensInHand(selectedAisles);
        List<Integer> selectedOrders = new ArrayList<>();
        // percorre os pedidos do com maior quantidade até o menor
        for(int i = 0; i < sOrders.size() ; i++){
            boolean acept = true;

                // percorre os itens do pedido
                for(Map.Entry<Integer, Integer> entry : sOrders.get(i).entrySet()){
                    //confere se podemos atenter aquele item
                    if (inHandItens[entry.getKey()] < entry.getValue()) {
                        acept = false;
                        break;
                    }
                }

            int[] auxItens = new int[nItems];
            calcAmountOfItens(sOrders.get(i), auxItens);
            if(acept && bound + sumTotalUnitsOrder[sumSTotalUnitsOrder[i].getKey()] > waveSizeUB) acept = false;
            // se todos os itens do pedido 'i' forem atenditos
            else if(acept){
                bound += sumTotalUnitsOrder[sumSTotalUnitsOrder[i].getKey()];
                // pedido adicionado a solução
                selectedOrders.add(sumSTotalUnitsOrder[i].getKey());
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
    List<Integer> selectReverseOrdersByAisles(List<Integer> selectedAisles){
        int bound = 0;
        // faz o calculo do total de itens presentes nestes corredores
        int[] inHandItens = generateItensInHand(selectedAisles);
        List<Integer> selectedOrders = new ArrayList<>();
        // percorre os pedidos do com maior quantidade até o menor
        for(int i = sOrders.size()-1; i >= 0 ; i--){
            boolean acept = true;

                // percorre os itens do pedido
                for(Map.Entry<Integer, Integer> entry : sOrders.get(i).entrySet()){
                    //confere se podemos atenter aquele item
                    if (inHandItens[entry.getKey()] < entry.getValue()) {
                        acept = false;
                        break;
                    }
                }

            int[] auxItens = new int[nItems];
            calcAmountOfItens(sOrders.get(i), auxItens);
            if(acept && bound + sumTotalUnitsOrder[sumSTotalUnitsOrder[i].getKey()] > waveSizeUB) acept = false;
                // se todos os itens do pedido 'i' forem atenditos
            else if(acept){
                bound += sumTotalUnitsOrder[sumSTotalUnitsOrder[i].getKey()];
                // pedido adicionado a solução
                selectedOrders.add(sumSTotalUnitsOrder[i].getKey());
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
    private double calcSolutionBound(List<Integer> selectedOrders){
        if(!selectedOrders.isEmpty()){
            int amountAisles = aisles.size();
            int itensInOrders = 0;
            for(int i : selectedOrders){
                itensInOrders += sumTotalUnitsOrder[i];
            }
            return (double) itensInOrders;
        }
        return 0;
    }
    private void initialize(){
        mostComumItem = 0;
        for(int i = 1; i < nItems; i++){
            if(amountItensOrders[i] > amountItensOrders[mostComumItem]) mostComumItem = i;
        }

        nOrders = orders.size();
        nAisles = aisles.size();
        //calcula a quantidade de itens em cada corredor e pedido
        sumItens(orders, sumSTotalUnitsOrder,sumTotalUnitsOrder);

        qtdMItens(aisles, sumSTotalUnitsAisle,sumTotalUnitsAisle);
        //ordena pedidos e corredores pela quantidade de itens presentes e salva em uma copia do vetor original
        heapSort(sOrders, sumSTotalUnitsOrder);
        heapSort(sAisles, sumSTotalUnitsAisle);
        //calcular a quantidade total dos itens nos corredores e pedidos
        calcTotalAmountOfItems(amountItensAisles, aisles);
        calcTotalAmountOfItems(amountItensOrders,orders);
        memo = new HashMap<>();
        //calcular o item que com maior presença nos pedidos


    }

    private ChallengeSolution initialSolution01(){
        List<Integer> solution = new ArrayList<>();
        int bound = 0;

        //adicione corredores na solução de acord com a limitação de UB
        for(int i = 0; i < sAisles.size(); i++){
            if(bound +sumTotalUnitsAisle[sumSTotalUnitsAisle[i].getKey()]  <= waveSizeUB){
                bound += sumTotalUnitsAisle[sumSTotalUnitsAisle[i].getKey()];
                solution.add(sumSTotalUnitsAisle[i].getKey());
            }
        }

        List<Integer> solution2 =  selectOrdersByAisles(solution);
        bound = 0;

        for(int i : solution2) bound += sumSTotalUnitsOrder[i].getValue();
        while(bound < waveSizeLB && solution.size() < sAisles.size()){
            bound = 0;
            solution.add(sumSTotalUnitsAisle[solution.size()].getKey());
            solution2 =  selectOrdersByAisles(solution);
            for(int i : solution2) bound += sumSTotalUnitsOrder[i].getValue();
        }
        if(bound > waveSizeUB ) System.out.println("deu caca");
        return new ChallengeSolution(new HashSet<>(solution2), new HashSet<>(solution));  //sol2 sol2 ???
    }

    private ChallengeSolution initialSolution02(){

        // Guardar a melhor solução
        List<Integer> bSelectedAisles = new ArrayList<>();
        List<Integer> bSelectedOrders = new ArrayList<>();
        double bestQuality = -1;
        //gerar solução valida inicial
        for(int i = 0; i < sAisles.size(); i++){
            bSelectedAisles.add(sumSTotalUnitsAisle[i].getKey());
        }
        bSelectedOrders = selectOrdersByAisles(bSelectedAisles);
        bestQuality = calcSolutionBound(bSelectedOrders)/bSelectedAisles.size();
        if(calcSolutionBound(bSelectedOrders) < waveSizeLB) bSelectedOrders = selectReverseOrdersByAisles(bSelectedAisles);
        // vetores auxiliates
        List<Integer> selectedOrdersAux = new ArrayList<>();
        List<Integer> selectedAislesAux = new ArrayList<>();
        List<Integer> selectedROrdersAux = new ArrayList<>();
        // bound da solução atual
        int bound = 0;

        for(int i = 0; i < sAisles.size(); i++){
            selectedAislesAux.add(sumSTotalUnitsAisle[i].getKey());
            if(bound + sumTotalUnitsAisle[sumSTotalUnitsAisle[i].getKey()] >= waveSizeLB){
                if(bound > waveSizeLB) break;
                selectedOrdersAux = selectOrdersByAisles(selectedAislesAux);
                selectedROrdersAux = selectReverseOrdersByAisles(selectedAislesAux);
                double auxRbound = calcSolutionBound(selectedROrdersAux);
                double qualityR = auxRbound / selectedAislesAux.size();
                double auxbound = calcSolutionBound(selectedOrdersAux);
                double quality = auxbound / selectedAislesAux.size();
                if(quality < qualityR && auxRbound >= waveSizeLB){
                    quality = qualityR;
                    auxbound = auxRbound;
                    selectedOrdersAux = selectedROrdersAux;
                }
                if(quality > bestQuality && auxbound >= waveSizeLB){
                    bestQuality = quality;
                    bSelectedAisles = selectedAislesAux;
                    bSelectedOrders = selectedOrdersAux;

                }
            }
        }
        if(selectedAislesAux.isEmpty()) return initialSolution01();
        return new ChallengeSolution(new HashSet<>(bSelectedOrders), new HashSet<>(bSelectedAisles));
    }
    private List<Integer> melhorSolução(List<Integer> s1, List<Integer> s2){
        if(calcSolutionBound(s1) > calcSolutionBound(s2)) return s1;
        else return s2;
    }
    private boolean podeAtender(int k, int [] estoque){
        int [] estoqueAux = new int[estoque.length];
        calcAmountOfItens(orders.get(k), estoqueAux);

        for(int i = 0; i <nItems;i++){
            if(estoqueAux[i] > estoque[i]){
                return false;
            }
        }
        for(int i = 0; i <nItems; i++){
            estoque[i] -= estoqueAux[i];
        }
        return true;
    }
    List<Integer> F(int k, int valAlvo, int[] estoque){
        if(k < 0 || valAlvo < 0) return new ArrayList<>(); // pedidos zerados ou meta atingida

        //chave de memorização


        // não incluir pedido atual
        List<Integer> aux1 = F(k-1, valAlvo, estoque);
        List<Integer> aux2 = new ArrayList<>();
        int valk = sumTotalUnitsOrder[k];
        int [] auxEstoque = Arrays.copyOf(estoque, estoque.length);
        if(podeAtender(k,auxEstoque)){
            aux2 = new ArrayList<>(F(k-1, valAlvo-valk, auxEstoque));
            aux2.add(k);
        }

        return melhorSolução(aux1, aux2);
    }
    public  List<Integer> fI(int valAlvo, int[] estoque) {
        int n = nOrders;
        List<Integer>[] dp = new ArrayList[valAlvo + 1];
        dp[0] = new ArrayList<>();

        for (int k = 0; k < n; k++) {
            int valk = sumTotalUnitsOrder[k];
            int[] auxEstoque = Arrays.copyOf(estoque, estoque.length);
            if (podeAtender(k, auxEstoque)) {
                for (int j = valAlvo; j >= valk; j--) {
                    if (dp[j - valk] != null) {
                        List<Integer> novaLista = new ArrayList<>(dp[j - valk]);
                        novaLista.add(k);
                        if (dp[j] == null || melhorSolução(dp[j], novaLista) == novaLista) {
                            dp[j] = novaLista;
                        }
                    }
                }
            }
        }

        for (int i = valAlvo; i >= 0; i--) {
            if (dp[i] != null) {
                return dp[i];
            }
        }
        return new ArrayList<>();
    }
    private ChallengeSolution solution3(){
        int bound = 0;
        List<Integer> solutionAisles = new ArrayList<>();
        for(int i = 0; i < sAisles.size(); i++){
            bound += sumTotalUnitsAisle[sumSTotalUnitsAisle[i].getKey()];
            if(bound > waveSizeUB) break;
            else if(bound > waveSizeLB + (waveSizeLB/2)) break;
            else{
                solutionAisles.add(sumSTotalUnitsAisle[i].getKey());
            }
        }
        List<Integer> solutionOrders = fI(waveSizeUB,generateItensInHand(solutionAisles));
        return new ChallengeSolution(new HashSet<>(solutionOrders), new HashSet<>(solutionAisles));
    }
    public ChallengeSolution solve(StopWatch stopWatch) {

        return solution3();
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
