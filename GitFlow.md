Git Flow
==========

1. PURPOSE
   -------
   

This document defines the Git workflow for the project so that:

main always contains stable, tested code.
Developers do not push directly to main.
Each iteration is developed separately.
Each developer works on a feature branch.
Feature branches are merged into the iteration branch through PRs.
The complete iteration is tested before it is merged into main.
Failed testing is fixed in the appropriate feature branch and tested again.

2. BRANCH STRUCTURE
   ----------------

                            MAIN
                  Production / Stable
                         │
                         │ Create Iteration 1
                         ▼
                  iteration-1
                Week 1 Development
                         │
   
          ┌──────────────┼──────────────┐
          │              │              │
          ▼
                            
   feature/login      feature/product     feature/cart
   
   Developer 1        Developer 2      Developer 3


          │              │              │
          └──────────────┼──────────────┘
                         │
   
                    Pull Requests
   
                         │
                         ▼
   
                  iteration-1
                Integration Testing
   
                         │
   
                    QA Testing
                         │
                  ┌──────┴──────┐
                  │             │
                PASS           FAIL
                  │             │
                  ▼             ▼
              Merge to       Fix in
                main         feature branch
                  │             │
                  │             └──────► Test Again
                  ▼
                 MAIN
                Stable
                  │
                  ▼
             iteration-2
                  │
             Repeat Process

4. DEVELOPER WORKFLOW
   ------------------
   
Step 1: Checkout the iteration branch
--------------------------------------
git checkout iteration-1

Step 2: Get the latest changes
----------------------------
git pull origin iteration-1

Step 3: Create a feature branch
-------------------------------
git checkout -b feature/login

Step 4: Develop the feature
----------------------------

Make the required code changes.

Step 5: Check changes
---------------------
git status

Step 6: Add changes
---------------------
git add .

Step 7: Commit changes
---------------------------
git commit -m "Add login functionality"

Step 8: Push the feature branch
--------------------------------
git push -u origin feature/login

Step 9: Create Pull Request
-----------------------------

Create a PR:

feature/login
       ↓
iteration-1
