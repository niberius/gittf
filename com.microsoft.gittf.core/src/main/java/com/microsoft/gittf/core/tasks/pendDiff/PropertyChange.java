/*
 *
 *
 *
 *
 */
import org.eclipse.jgit.lib.FileMode;
import org.eclipse.jgit.lib.ObjectId;
        extends Change {
     *
     * @param path     the item path in the git repository
     * @param objectID the object id of the item
     * @param mode     the file mode of the item
    public PropertyChange(final String path, final ObjectId objectID, final FileMode mode) {
     *
     * @param path     the item path in the git repository
     * @param objectID the object id of the item
     * @param oldMode  the old file mode of the item
     * @param newMode  the new file mode of the item
    public PropertyChange(final String path, final ObjectId objectID, final FileMode oldMode, final FileMode newMode) {
    public boolean isExecutablePropertyChanged() {
    public boolean isPropertyChanged() {
    public PropertyValue getExecutablePropertyValue() {
        if (isExecutable()) {
        } else {
    private boolean isExecutable() {