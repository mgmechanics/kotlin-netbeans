/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package installer;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.jetbrains.kotlin.installer.Yenta;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Michael Groß
 */
public final class YentaTest {
/* Everything was done with Netbeans 12.2 (running on Java 11)
After fixing the NullpointerException in Yenta#validate line 120
    - when installing the plugin there was a message from Netbeans that a timeout occurred
    - when launching Netbeans with a Kotlin file open Netbeans tried to launch the Kotlin plugin but this process did never finish (inifinite loop?)
    
When executing this test on Netbeans opens a pop-up Window containing the following text:
    
Warning - could not install some modules: 
    Extensible Abstract Model (XAM) - No module providing the capability org.netbeans.modules.xml.xam.spi.ModelAccessProvider could be found. 
    Extensible Abstract Model (XAM) - No module providing the capability org.netbeans.modules.xml.xam.spi.DocumentModelAccessProvider could be found. 
    Static Analysis Core - No module providing the capability org.openide.windows.WindowManager could be found. 
    Enhanced Web Browser Functionality - No module providing the capability org.openide.windows.WindowManager could be found. 
    Maven Embedder - The module named org.apache.commons.logging was needed and not found. 
    Java Project Support - No module providing the capability javax.script.ScriptEngine.freemarker could be found. 
    Java Project Support UI - No module providing the capability javax.script.ScriptEngine.freemarker could be found. 
    Maven Projects - The module named org.apache.commons.codec was needed and not found. 
    JUnit Tests - No module providing the capability javax.script.ScriptEngine.freemarker could be found. 
    JPDA Debugger - None of the modules providing the capability org.netbeans.spi.debugger.jpda.SourcePathProvider could be installed. J
    PDA Debugger - None of the modules providing the capability org.netbeans.spi.debugger.jpda.EditorContext could be installed. 
    Kotlin - No module providing the capability javax.script.ScriptEngine.freemarker could be found. 
    Java SE Projects - The module named org.netbeans.modules.java.api.common/0-1 was needed and not found. 
    20 further modules could not be installed due to the above problems.
*/
    
    @Test
    public void testValid() {
        TestYenta yenta = new TestYenta();
        yenta.validate();
        assertTrue(true);
    }

    private static class TestYenta extends Yenta {
        /**
         * From org.jetbrains.kotlin.installer.KotlinInstaller
         */
        @Override
        public Set<String> friends() {
            return setOf(
                "org.netbeans.modules.maven",
                "org.netbeans.modules.maven.embedder",
                "org.netbeans.modules.jumpto",
                "org.netbeans.modules.debugger.jpda",
                "org.netbeans.modules.debugger.jpda.projects",
                "org.netbeans.modules.java.api.common",
                "org.netbeans.modules.java.preprocessorbridge"
            );
        }
    }
    
    private static Set<String> setOf(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }
}