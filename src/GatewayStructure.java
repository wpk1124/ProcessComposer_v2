public class GatewayStructure {
    private int[] edgeGateways;
    private int[][] gatewayConnections;
    private int[] gatewayTypes;
    private int outputGateway;

    public GatewayStructure(int[] eg, int[][] gc, int[] gt, int og) {
        edgeGateways = eg.clone();
        gatewayConnections = gc.clone();
        gatewayTypes = gt.clone();
        outputGateway = og;
    }

    public void printGatewayStructure() {
        System.out.println("eg");
        for(int e : this.getEdgeGateways()) {
            System.out.print(e+",");
        }
        System.out.println("\n");
        System.out.println("gc");
        for(int[] e : this.getGatewayConnections()) {
            for(int f : e) {
                System.out.print(f+",");
            }
            System.out.println("\n");
        }
        System.out.println("gt");
        for(int e : this.getGatewayTypes()) {
            System.out.print(e+",");
        }
        System.out.println("\n");
        System.out.println("og: "+outputGateway);
    }

    public int[] getEdgeGateways() {return edgeGateways;}
    public int[][] getGatewayConnections() {return gatewayConnections;}
    public int[] getGatewayTypes() {return gatewayTypes;}
    public int getOutputGateway() {return outputGateway;}
}
