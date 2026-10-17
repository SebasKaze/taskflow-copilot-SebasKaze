src/test/java/com/taskflow/slice/BusquedaTareasControllerTest.java

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;

Low
Usar @MockitoBean en lugar de @MockBean en el test
No bloqueante: specs/search.md:55–58 pide @MockitoBean, como el slice existente src/test/java/com/taskflow/slice/TaskControllerTest.java:21,67–75. Este test usa @MockBean en su lugar. Sustituye esta importación por org.springframework.test.context.bean.override.mockito.MockitoBean y cambia las tres anotaciones a @MockitoBean para mantener la convención del proyecto.