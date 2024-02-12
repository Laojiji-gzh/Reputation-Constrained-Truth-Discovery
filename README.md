# RCTD: Reputation-Constrained Truth Discovery in Sybil Attack Crowdsourcing Environment



### 🚩Running the code

The code can run in an integrated development environment (IDE) or via command-line execution.

**Run with IDE:**

The main class: Run.java



**Run with command-line:**

compile: (not needed):

`javac -encoding utf-8 -cp "lib/commons-math3-3.6.1.jar" src/*.java src/Utils/*.java src/Entity/*.java`

run: (an example)

`java -cp "src;lib/commons-math3-3.6.1.jar" Run DOG 0.4 0.1 1 0.25 0.9 2 10 100 1.5 0.15 true `



**Parameter Description**

java -cp "src;lib/commons-math3-3.6.1.jar" Run dataset $\mu$ $\epsilon$ $\lambda$ $\theta$ $p$ $step$ $iteration$ $batch$ $k$ $m$ use_wilson

| Parameter  |                         Description                         |      Setting      |
| :--------: | :---------------------------------------------------------: | :---------------: |
|  dataset   |                        dataset name                         | DOG WS SP PosSent |
|   $\mu$    |                  Sybil account proportion                   |      0 - 0.6      |
| $\epsilon$ | The probability of Sybil account submit label Independently |      0 - 0.4      |
| $\lambda$  |                      Attacker entities                      |        1-4        |
|  $\theta$  |              Average accuracy of Sybil workers              |    0.15 - 0.45    |
|     p      |                       decay parameter                       |    0.8 - 0.99     |
|    step    |                            step                             |        1-4        |
| iteration  |                              -                              |      Integer      |
|   batch    |                              -                              |      Integer      |
|     k      |                         $\Delta k$                          |      0.5 - 2      |
|     m      |                         $\Delta m$                          |    0.05 - 0.2     |
| use_wilson |    Is use Wilson Interval Score refine the approval rate    |    true/false     |

