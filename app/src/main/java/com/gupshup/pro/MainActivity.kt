        btnGreedyGame.setOnClickListener {
            openGreedyMinigame()
        }
        
        btnGreedyGame.setOnClickListener {
            startActivity(Intent(this, GreedyGameActivity::class.java))
        }
        
