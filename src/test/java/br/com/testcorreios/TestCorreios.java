package br.com.testcorreios;

import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.Iterator;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;


@TestMethodOrder(OrderAnnotation.class)
@SpringBootTest
class TestCorreios {
	
	boolean contarTempo;
    long marcador = 0;
	private RestTemplate restTemplate;
	
	
	@BeforeEach
	public void runBeforeAllTestMethods() {
		restTemplate = new RestTemplate();
	}

	@Order(1)
	@Test
	public void simpleTest() {		
		
		String endpoint = endPoint("69906380", "59010160", "20", "20", "20", "0,8");
		
		ResponseEntity<String> response = 
			      restTemplate.getForEntity("http://ws.correios.com.br/calculador/CalcPrecoPrazo.aspx?"
			    		 + endpoint , String.class);
	
		String[] values = getValues(response.getBody());
		
		String preco = values[0];
		String prazo = values[1];

		assertEquals(preco, "54,40");
		assertEquals(prazo, "19");
	}

	@Order(2)
	@Test
	public void massTest() throws FileNotFoundException, IOException, ParseException {
	
		JSONParser jsonParser = new JSONParser();
		JSONArray jsonArray = (JSONArray) jsonParser.parse(new FileReader("correios.json"));
		Iterator<JSONObject> iterator = jsonArray.iterator();
		
		contarTempo();
		while(iterator.hasNext()) {
			
			JSONObject obj = (JSONObject) iterator.next();
			
			String endpoint = endPoint((String) obj.get("sCepDestino"), (String) obj.get("sCepOrigem"), 
										(String) obj.get("nVlAltura"), (String) obj.get("nVlLargura"),
										(String) obj.get("nVlComprimento"), (String) obj.get("nVlPeso"));
		
			ResponseEntity<String> response = 
				      restTemplate.getForEntity("http://ws.correios.com.br/calculador/CalcPrecoPrazo.aspx?"
				    		 + endpoint , String.class);
			
			String[] values = getValues(response.getBody());
			
			String preco = values[0];
			String prazo = values[1];
			
			if(contarTempo() <= 5000) {
				assertEquals(preco, (String) obj.get("preco"));
				assertEquals(prazo, (String) obj.get("prazo"));
			}
				
		}
		
	}
	
	
	private String endPoint(String dest, String origem, String altura, String largura, 
							String comprimento, String peso) {
		
		return "nCdServico=04510&nCdEmpresa&sDsSenha&sCepDestino="+dest+"&sCepOrigem="+origem+"&"
	      		+ "nVlAltura="+altura+"&nVlLargura="+largura+"&nVlDiametro=0"
	      		+ "&nVlComprimento="+comprimento+"&nVlPeso="+peso+"&nCdFormato=1&"
	      		+ "sCdMaoPropria=N&nVlValorDeclarado=0&sCdAvisoRecebimento=N&StrRetorno=xml";
	}
	
	
    private String[] getValues(String response) {
    	
    	try {
	    	DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
	        DocumentBuilder db = dbf.newDocumentBuilder();
	        InputSource is = new InputSource();
	        is.setCharacterStream(new StringReader(response));
	        Document doc = db.parse(is);
	        Element element = doc.getDocumentElement();
	        NodeList cServico = element.getChildNodes();
	        Node codigo = cServico.item(0).getFirstChild();
	        Node valor = codigo.getNextSibling();
	        Node prazoEntrega = valor.getNextSibling();
	        String [] values = {valor.getTextContent(), prazoEntrega.getTextContent()};
	        return values;
    	}
    	catch(ParserConfigurationException | SAXException | IOException e) {
    		e.printStackTrace();
    	}
        return null;
    }
    
    public long contarTempo() {
        
        if (!contarTempo) {
        marcador = System.currentTimeMillis();
        contarTempo=true;
       }
       return System.currentTimeMillis() - marcador;
    }
    
	
}