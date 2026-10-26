package vn.cfop.rubik

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import vn.cfop.rubik.solver.CfopSolver
import vn.cfop.rubik.solver.Cube
import vn.cfop.rubik.ui.InputScreen
import vn.cfop.rubik.ui.SolveScreen
import vn.cfop.rubik.ui.blankState

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                var showSolve by remember { mutableStateOf(false) }
                var cubeState by remember { mutableStateOf(blankState()) }
                var stages by remember { mutableStateOf<List<CfopSolver.Stage>>(emptyList()) }

                if (showSolve) {
                    SolveScreen(
                        initialState = cubeState,
                        stages = stages,
                        onBack = { showSolve = false },
                        onNewCube = {
                            cubeState = blankState()
                            stages = emptyList()
                            showSolve = false
                        },
                    )
                } else {
                    InputScreen(
                        initialState = cubeState,
                        onSolved = { state, solvedStages ->
                            cubeState = state
                            stages = solvedStages
                            showSolve = true
                        },
                    )
                }
            }
        }
    }
}
