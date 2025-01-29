for i in $(seq -f "%04g" 1 20); do
    echo $i
    python3 checker.py datasets/a/instance_${i}.txt resultados/instance_${i}.txt
done
