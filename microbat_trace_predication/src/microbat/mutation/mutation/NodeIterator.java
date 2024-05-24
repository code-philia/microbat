package microbat.mutation.mutation;

import japa.parser.ast.Node;

public class NodeIterator {
    public interface NodeHandler {
        boolean handle(Node astNode);
    }

    private NodeHandler nodeHandler;

    public NodeIterator(NodeHandler nodeHandler) {
        this.nodeHandler = nodeHandler;
    }

    public void explore(Node astNode) {
        if (nodeHandler.handle(astNode)) {
            for (Node child : astNode.getChildrenNodes()) {
                explore(child);
            }
        }
    }
}
